# Project Configuration
variable "project_name" {
  description = "Project name"
  type        = string
  default     = "ziboto"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

# VPC Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

# EC2 Backend Configuration
variable "ec2_instance_type" {
  description = "EC2 instance type for backend"
  type        = string
  default     = "t3.medium"
}

variable "ec2_ami_id" {
  description = "AMI ID for EC2 instances (Amazon Linux 2023)"
  type        = string
  default     = "" # Will use latest AL2023 if empty
}

variable "ec2_min_size" {
  description = "Minimum number of EC2 instances"
  type        = number
  default     = 2
}

variable "ec2_max_size" {
  description = "Maximum number of EC2 instances"
  type        = number
  default     = 10
}

variable "ec2_desired_size" {
  description = "Desired number of EC2 instances"
  type        = number
  default     = 2
}

variable "backend_port" {
  description = "Backend application port"
  type        = number
  default     = 8080
}

# RDS Configuration
variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "rds_allocated_storage" {
  description = "RDS allocated storage (GB)"
  type        = number
  default     = 20
}

variable "rds_max_allocated_storage" {
  description = "RDS max allocated storage for autoscaling (GB)"
  type        = number
  default     = 100
}

variable "rds_backup_retention_days" {
  description = "RDS backup retention period"
  type        = number
  default     = 7
}

variable "rds_multi_az" {
  description = "Enable Multi-AZ for RDS"
  type        = bool
  default     = false # Set to true for production
}

# ElastiCache Redis Configuration
variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "cache.t3.micro"
}

variable "redis_num_cache_nodes" {
  description = "Number of cache nodes"
  type        = number
  default     = 1
}

# Amazon MQ RabbitMQ Configuration
variable "amazonmq_instance_type" {
  description = "Amazon MQ instance type"
  type        = string
  default     = "mq.t3.micro"
}

variable "amazonmq_deployment_mode" {
  description = "Amazon MQ deployment mode (SINGLE_INSTANCE or ACTIVE_STANDBY_MULTI_AZ)"
  type        = string
  default     = "SINGLE_INSTANCE" # Use ACTIVE_STANDBY_MULTI_AZ for production
}

# S3 Configuration
variable "s3_bucket_name" {
  description = "S3 bucket name for file storage"
  type        = string
}

variable "s3_enable_versioning" {
  description = "Enable S3 versioning"
  type        = bool
  default     = false
}

variable "s3_lifecycle_days" {
  description = "Days before transitioning to INTELLIGENT_TIERING"
  type        = number
  default     = 90
}

# Monitoring Configuration
variable "enable_monitoring" {
  description = "Enable CloudWatch monitoring"
  type        = bool
  default     = true
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 7
}

# Budget Configuration
variable "monthly_budget_limit" {
  description = "Monthly budget limit in USD"
  type        = number
  default     = 100
}

variable "budget_alert_emails" {
  description = "Email addresses for budget alerts"
  type        = list(string)
  default     = []
}

# Domain Configuration (Optional)
variable "domain_name" {
  description = "Domain name for the application"
  type        = string
  default     = ""
}

variable "certificate_arn" {
  description = "ACM certificate ARN for HTTPS"
  type        = string
  default     = ""
}

# Application Configuration
variable "jwt_secret" {
  description = "JWT secret key (base64 encoded, 256+ bits)"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "rabbitmq_password" {
  description = "RabbitMQ password"
  type        = string
  sensitive   = true
}

variable "google_client_id" {
  description = "Google OAuth Client ID"
  type        = string
  default     = ""
}

variable "google_client_secret" {
  description = "Google OAuth Client Secret"
  type        = string
  sensitive   = true
  default     = ""
}
