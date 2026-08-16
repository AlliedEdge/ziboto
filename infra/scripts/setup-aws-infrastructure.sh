#!/bin/bash

# AWS Infrastructure Setup Script
# This script provisions AWS infrastructure using Terraform

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT="${1:-production}"
AWS_REGION="${2:-us-east-1}"

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}Ziboto AWS Infrastructure Setup${NC}"
echo -e "${GREEN}Environment: ${ENVIRONMENT}${NC}"
echo -e "${GREEN}Region: ${AWS_REGION}${NC}"
echo -e "${GREEN}=====================================${NC}"

# Check prerequisites
echo -e "\n${YELLOW}Checking prerequisites...${NC}"

if ! command -v terraform &> /dev/null; then
    echo -e "${RED}Terraform is not installed. Please install it first.${NC}"
    exit 1
fi

if ! command -v aws &> /dev/null; then
    echo -e "${RED}AWS CLI is not installed. Please install it first.${NC}"
    exit 1
fi

if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}kubectl is not installed. Please install it first.${NC}"
    exit 1
fi

# Verify AWS credentials
if ! aws sts get-caller-identity &> /dev/null; then
    echo -e "${RED}AWS credentials not configured. Please run 'aws configure'.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites check passed${NC}"

# Navigate to Terraform directory
cd ../terraform/aws

# Initialize Terraform
echo -e "\n${YELLOW}Initializing Terraform...${NC}"
terraform init
echo -e "${GREEN}✓ Terraform initialized${NC}"

# Validate Terraform configuration
echo -e "\n${YELLOW}Validating Terraform configuration...${NC}"
terraform validate
echo -e "${GREEN}✓ Configuration validated${NC}"

# Create tfvars file if it doesn't exist
if [ ! -f "${ENVIRONMENT}.tfvars" ]; then
    echo -e "\n${YELLOW}Creating ${ENVIRONMENT}.tfvars file...${NC}"
    cat > ${ENVIRONMENT}.tfvars <<EOF
# Project Configuration
project_name = "ziboto"
environment  = "${ENVIRONMENT}"
aws_region   = "${AWS_REGION}"

# VPC Configuration
vpc_cidr = "10.0.0.0/16"
availability_zones = ["${AWS_REGION}a", "${AWS_REGION}b", "${AWS_REGION}c"]

# EKS Configuration
eks_cluster_version = "1.28"
eks_node_instance_types = ["t3.large"]
eks_node_desired_size = 3
eks_node_min_size = 2
eks_node_max_size = 10

# RDS Configuration
db_instance_class = "db.t3.medium"
db_name = "ziboto"
db_username = "ziboto"
db_password = "CHANGE_ME_PLEASE"  # Change this!
db_allocated_storage = 100
db_max_allocated_storage = 500
db_backup_retention_period = 30

# ElastiCache Configuration
redis_node_type = "cache.t3.medium"
redis_num_cache_nodes = 2

# S3 Configuration
s3_bucket_name = "ziboto-files-\${AWS::AccountId}-${AWS_REGION}"
s3_enable_versioning = true
s3_lifecycle_days = 90

# Domain Configuration
domain_name = "ziboto.com"

# Monitoring
enable_monitoring = true
log_retention_days = 30
EOF
    echo -e "${RED}Please update ${ENVIRONMENT}.tfvars with your actual values, especially the database password!${NC}"
    echo -e "${YELLOW}Press Enter to continue after updating the file...${NC}"
    read
fi

# Show Terraform plan
echo -e "\n${YELLOW}Generating Terraform plan...${NC}"
terraform plan -var-file="${ENVIRONMENT}.tfvars" -out=tfplan
echo -e "${GREEN}✓ Plan generated${NC}"

# Ask for confirmation
echo -e "\n${YELLOW}Do you want to apply these changes? (yes/no)${NC}"
read -r confirmation
if [ "$confirmation" != "yes" ]; then
    echo -e "${RED}Deployment cancelled.${NC}"
    exit 0
fi

# Apply Terraform
echo -e "\n${YELLOW}Applying Terraform configuration...${NC}"
terraform apply tfplan
echo -e "${GREEN}✓ Infrastructure provisioned${NC}"

# Get outputs
echo -e "\n${YELLOW}Retrieving infrastructure outputs...${NC}"
EKS_CLUSTER_NAME=$(terraform output -raw eks_cluster_id)
AWS_REGION=$(terraform output -raw aws_region || echo $AWS_REGION)
RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
REDIS_ENDPOINT=$(terraform output -raw elasticache_primary_endpoint)
S3_BUCKET=$(terraform output -raw s3_bucket_name)

# Configure kubectl
echo -e "\n${YELLOW}Configuring kubectl...${NC}"
aws eks update-kubeconfig --name ${EKS_CLUSTER_NAME} --region ${AWS_REGION}
echo -e "${GREEN}✓ kubectl configured${NC}"

# Verify cluster connectivity
echo -e "\n${YELLOW}Verifying cluster connectivity...${NC}"
kubectl cluster-info
kubectl get nodes
echo -e "${GREEN}✓ Cluster is accessible${NC}"

# Create .env file for Kubernetes
echo -e "\n${YELLOW}Creating environment configuration...${NC}"
cat > ../../.env.k8s <<EOF
# Database Configuration
DATABASE_URL=jdbc:postgresql://${RDS_ENDPOINT}/ziboto
DATABASE_USERNAME=ziboto
DATABASE_PASSWORD=$(terraform output -raw rds_password)

# Redis Configuration
REDIS_HOST=${REDIS_ENDPOINT}
REDIS_PORT=6379
REDIS_PASSWORD=$(aws secretsmanager get-secret-value --secret-id ziboto-${ENVIRONMENT}-redis-auth-token --query SecretString --output text)

# S3 Configuration
AWS_S3_BUCKET=${S3_BUCKET}
AWS_REGION=${AWS_REGION}

# JWT Secret
JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id ziboto-${ENVIRONMENT}-jwt-secret --query SecretString --output text)

# Add other secrets manually
RESEND_API_KEY=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
EOF

echo -e "${GREEN}✓ Environment configuration created at ../../.env.k8s${NC}"

# Display summary
echo -e "\n${GREEN}=====================================${NC}"
echo -e "${GREEN}Infrastructure Setup Complete!${NC}"
echo -e "${GREEN}=====================================${NC}"
echo -e "\n${YELLOW}Infrastructure Details:${NC}"
echo -e "  EKS Cluster: ${EKS_CLUSTER_NAME}"
echo -e "  RDS Endpoint: ${RDS_ENDPOINT}"
echo -e "  Redis Endpoint: ${REDIS_ENDPOINT}"
echo -e "  S3 Bucket: ${S3_BUCKET}"
echo -e "\n${YELLOW}Next Steps:${NC}"
echo -e "  1. Update ../../.env.k8s with remaining secrets"
echo -e "  2. Run the deployment script: ./deploy-k8s.sh ${ENVIRONMENT}"
echo -e "\n${YELLOW}To tear down infrastructure:${NC}"
echo -e "  cd ../terraform/aws && terraform destroy -var-file=${ENVIRONMENT}.tfvars"
