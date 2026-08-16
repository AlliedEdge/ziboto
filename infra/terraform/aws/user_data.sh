#!/bin/bash
set -e

# Update system
dnf update -y

# Install Java 21
# dnf install -y java-21-amazon-corretto-devel wget curl
dnf install -y java-21-amazon-corretto-devel wget

# Install CloudWatch agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/arm64/latest/amazon-cloudwatch-agent.rpm

rpm -U ./amazon-cloudwatch-agent.rpm

# Create application directory
mkdir -p /opt/ziboto
cd /opt/ziboto

# Download application JAR from S3 (you'll upload it separately)
aws s3 cp s3://${s3_bucket}/deploy/ziboto-backend.jar /opt/ziboto/app.jar

# Create environment file
cat > /opt/ziboto/.env <<EOF
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=${backend_port}

# Database
DATABASE_URL=jdbc:postgresql://${database_endpoint}/${database_name}
DATABASE_USERNAME=${database_username}
DATABASE_PASSWORD=${database_password}
DATABASE_POOL_SIZE=20

# Redis
REDIS_HOST=${redis_endpoint}
REDIS_PORT=${redis_port}
REDIS_PASSWORD=
REDIS_DATABASE=0

# RabbitMQ
RABBITMQ_HOST=${rabbitmq_endpoint}
RABBITMQ_PORT=5671
RABBITMQ_USERNAME=${rabbitmq_username}
RABBITMQ_PASSWORD=${rabbitmq_password}
RABBITMQ_VHOST=/
RABBITMQ_USE_SSL=true

# AWS
AWS_REGION=${aws_region}
AWS_S3_BUCKET=${s3_bucket}
STORAGE_TYPE=s3

# JWT
JWT_SECRET=${jwt_secret}
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# CORS
CORS_ALLOWED_ORIGINS=https://ziboto.alliededge.app,http://localhost:5173

# Google OAuth
GOOGLE_CLIENT_ID=${google_client_id}
GOOGLE_CLIENT_SECRET=${google_client_secret}
OAUTH_REDIRECT_URL=https://ziboto.alliededge.app/oauth/callback

# Logging
LOG_LEVEL=INFO
LOG_FILE=/var/log/ziboto/app.log
EOF

# Create systemd service
cat > /etc/systemd/system/ziboto.service <<EOF
[Unit]
Description=Ziboto Backend Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/ziboto
EnvironmentFile=/opt/ziboto/.env
# ExecStart=/usr/bin/java -jar -Xms512m -Xmx1024m /opt/ziboto/app.jar
ExecStart=/usr/bin/java -Xms256m -Xmx600m -jar /opt/ziboto/app.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=ziboto

[Install]
WantedBy=multi-user.target
EOF

# Create log directory
mkdir -p /var/log/ziboto
chown ec2-user:ec2-user /var/log/ziboto

# Configure CloudWatch agent
cat > /opt/aws/amazon-cloudwatch-agent/etc/config.json <<EOF
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/ziboto/app.log",
            "log_group_name": "/aws/ec2/ziboto-backend",
            "log_stream_name": "{instance_id}"
          }
        ]
      }
    }
  },
  "metrics": {
    "namespace": "Ziboto/Backend",
    "metrics_collected": {
      "cpu": {
        "measurement": [
          {"name": "cpu_usage_idle", "rename": "CPU_IDLE", "unit": "Percent"},
          {"name": "cpu_usage_iowait", "rename": "CPU_IOWAIT", "unit": "Percent"},
          "cpu_time_guest"
        ],
        "metrics_collection_interval": 60,
        "totalcpu": false
      },
      "disk": {
        "measurement": [
          {"name": "used_percent", "rename": "DISK_USED", "unit": "Percent"}
        ],
        "metrics_collection_interval": 60,
        "resources": ["*"]
      },
      "mem": {
        "measurement": [
          {"name": "mem_used_percent", "rename": "MEM_USED", "unit": "Percent"}
        ],
        "metrics_collection_interval": 60
      }
    }
  }
}
EOF

# Start CloudWatch agent
/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/config.json

# Enable and start application
systemctl daemon-reload
systemctl enable ziboto
systemctl start ziboto

# Wait for application to be healthy
for i in {1..30}; do
  if curl -f http://localhost:${backend_port}/actuator/health > /dev/null 2>&1; then
    echo "Application is healthy"
    break
  fi
  echo "Waiting for application to start... ($i/30)"
  sleep 10
done
