# VPC Outputs
output "vpc_id" {
  description = "The ID of the VPC"
  value       = module.vpc.vpc_id
}

output "private_subnets" {
  description = "List of IDs of private subnets"
  value       = module.vpc.private_subnets
}

output "public_subnets" {
  description = "List of IDs of public subnets"
  value       = module.vpc.public_subnets
}

# ALB Outputs
output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = aws_lb.main.zone_id
}

output "alb_arn" {
  description = "ARN of the Application Load Balancer"
  value       = aws_lb.main.arn
}

# EC2 Auto Scaling Outputs
output "autoscaling_group_name" {
  description = "Name of the Auto Scaling Group"
  value       = aws_autoscaling_group.backend.name
}

output "launch_template_id" {
  description = "ID of the launch template"
  value       = aws_launch_template.backend.id
}

# RDS Outputs
output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.postgres.endpoint
}

output "rds_address" {
  description = "RDS instance address"
  value       = aws_db_instance.postgres.address
}

output "rds_port" {
  description = "RDS instance port"
  value       = aws_db_instance.postgres.port
}

output "rds_database_name" {
  description = "RDS database name"
  value       = aws_db_instance.postgres.db_name
}

# ElastiCache Outputs
output "redis_endpoint" {
  description = "Redis primary endpoint"
  value       = aws_elasticache_cluster.redis.cache_nodes[0].address
}

output "redis_port" {
  description = "Redis port"
  value       = aws_elasticache_cluster.redis.cache_nodes[0].port
}

# Amazon MQ Outputs
output "rabbitmq_endpoint" {
  description = "Amazon MQ RabbitMQ endpoint"
  value       = aws_mq_broker.rabbitmq.instances[0].endpoints[0]
}

output "rabbitmq_console_url" {
  description = "Amazon MQ console URL"
  value       = aws_mq_broker.rabbitmq.instances[0].console_url
}

# S3 Outputs
output "s3_bucket_name" {
  description = "S3 bucket name for file storage"
  value       = local.s3_bucket_id
}

output "s3_bucket_arn" {
  description = "S3 bucket ARN"
  value       = local.s3_bucket_arn
}

output "s3_bucket_domain_name" {
  description = "S3 bucket domain name"
  value       = data.aws_s3_bucket.storage.bucket_domain_name
}

# IAM Outputs
output "backend_iam_role_arn" {
  description = "IAM role ARN for backend EC2 instances"
  value       = aws_iam_role.backend.arn
}

output "backend_instance_profile_name" {
  description = "IAM instance profile name for backend"
  value       = aws_iam_instance_profile.backend.name
}

output "github_actions_deployment_role_arn" {
  description = "IAM role ARN for GitHub Actions OIDC deployment (set this as the AWS_DEPLOYMENT_ROLE_ARN repository secret)"
  value       = aws_iam_role.github_actions_deploy.arn
}

# Security Group Outputs
output "alb_security_group_id" {
  description = "Security group ID for ALB"
  value       = aws_security_group.alb.id
}

output "backend_security_group_id" {
  description = "Security group ID for backend instances"
  value       = aws_security_group.backend.id
}

# Connection Information
output "application_url" {
  description = "Application URL (use this to access the application)"
  value       = var.certificate_arn != "" ? "https://${aws_lb.main.dns_name}" : "http://${aws_lb.main.dns_name}"
}

output "connection_strings" {
  description = "Connection strings for all services"
  value = {
    database_url = "postgresql://ziboto:${nonsensitive(var.db_password)}@${aws_db_instance.postgres.endpoint}/ziboto"
    redis_url    = "redis://${aws_elasticache_cluster.redis.cache_nodes[0].address}:${aws_elasticache_cluster.redis.cache_nodes[0].port}"
    rabbitmq_url = aws_mq_broker.rabbitmq.instances[0].endpoints[0]
    s3_bucket    = local.s3_bucket_id
  }
  sensitive = true
}

# Cost Management
output "monthly_budget_limit" {
  description = "Monthly budget limit in USD"
  value       = var.monthly_budget_limit
}

# Instructions
output "deployment_instructions" {
  description = "Next steps for deployment"
  value       = <<-EOT
    
    ========================================
    ZIBOTO INFRASTRUCTURE DEPLOYED
    ========================================
    
    Application URL: ${var.certificate_arn != "" ? "https://${aws_lb.main.dns_name}" : "http://${aws_lb.main.dns_name}"}
    
    NEXT STEPS:
    
    1. Build and upload backend JAR:
       cd apps/backend
       ./mvnw clean package -DskipTests
       aws s3 cp target/backend-0.0.1-SNAPSHOT.jar s3://${local.s3_bucket_id}/deploy/ziboto-backend.jar
    
    2. Terminate and replace EC2 instances to pick up new JAR:
       aws autoscaling set-desired-capacity --auto-scaling-group-name ${aws_autoscaling_group.backend.name} --desired-capacity 0
       # Wait 2 minutes
       aws autoscaling set-desired-capacity --auto-scaling-group-name ${aws_autoscaling_group.backend.name} --desired-capacity 2
    
    3. Frontend is deployed separately to https://ziboto.alliededge.app
    
    4. CORS_ALLOWED_ORIGINS is set to https://ziboto.alliededge.app,http://localhost:5173 in user_data.sh
    
    RESOURCE MANAGEMENT:
    - To stop backend instances: Set ASG desired capacity to 0
    - To stop RDS: Use AWS console (can stop for 7 days)
    - ElastiCache and Amazon MQ: Delete when not in use (data will be lost)
    
    MONITORING:
    - CloudWatch Logs: /aws/ec2/ziboto-backend
    - Budget Alerts: Configured for ${var.monthly_budget_limit} USD/month
    
    ========================================
  EOT
}
