# Terraform Infrastructure

This directory contains Terraform configurations for provisioning cloud infrastructure for Ziboto on AWS, GCP, and Azure.

## Directory Structure

```
terraform/
├── aws/                   # AWS infrastructure
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   ├── eks.tf           # EKS cluster
│   ├── vpc.tf           # VPC and networking
│   ├── rds.tf           # RDS PostgreSQL
│   ├── elasticache.tf   # Redis cluster
│   ├── s3.tf            # S3 buckets
│   └── iam.tf           # IAM roles and policies
├── gcp/                   # GCP infrastructure
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   ├── gke.tf           # GKE cluster
│   ├── vpc.tf           # VPC network
│   ├── sql.tf           # Cloud SQL
│   └── storage.tf       # Cloud Storage
├── azure/                 # Azure infrastructure
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   ├── aks.tf           # AKS cluster
│   ├── vnet.tf          # Virtual Network
│   └── database.tf      # Azure Database
└── modules/               # Reusable modules
    ├── kubernetes/
    ├── monitoring/
    └── networking/
```

## Prerequisites

- Terraform >= 1.5.0
- AWS CLI, gcloud CLI, or Azure CLI configured
- kubectl installed
- Cloud provider credentials configured

## Quick Start

### AWS Infrastructure

```bash
cd aws

# Initialize Terraform
terraform init

# Plan changes
terraform plan -var-file="production.tfvars"

# Apply changes
terraform apply -var-file="production.tfvars"

# Get EKS cluster credentials
aws eks update-kubeconfig --name ziboto-eks --region us-east-1
```

### GCP Infrastructure

```bash
cd gcp

# Initialize Terraform
terraform init

# Plan changes
terraform plan -var-file="production.tfvars"

# Apply changes
terraform apply -var-file="production.tfvars"

# Get GKE cluster credentials
gcloud container clusters get-credentials ziboto-gke --region us-central1
```

### Azure Infrastructure

```bash
cd azure

# Initialize Terraform
terraform init

# Plan changes
terraform plan -var-file="production.tfvars"

# Apply changes
terraform apply -var-file="production.tfvars"

# Get AKS cluster credentials
az aks get-credentials --resource-group ziboto-rg --name ziboto-aks
```

## Environment Variables

Create a `terraform.tfvars` file or use environment variables:

```hcl
# Common
project_name = "ziboto"
environment  = "production"
region       = "us-east-1"

# Database
db_instance_class = "db.t3.medium"
db_username       = "ziboto"
db_name          = "ziboto"

# Kubernetes
k8s_node_count    = 3
k8s_node_type     = "t3.large"

# Networking
vpc_cidr = "10.0.0.0/16"
```

## Terraform State

For production, use remote state backend:

### AWS S3 Backend

```hcl
terraform {
  backend "s3" {
    bucket         = "ziboto-terraform-state"
    key            = "production/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}
```

### GCP GCS Backend

```hcl
terraform {
  backend "gcs" {
    bucket = "ziboto-terraform-state"
    prefix = "production"
  }
}
```

### Azure Storage Backend

```hcl
terraform {
  backend "azurerm" {
    resource_group_name  = "ziboto-terraform"
    storage_account_name = "zibototfstate"
    container_name       = "tfstate"
    key                  = "production.terraform.tfstate"
  }
}
```

## Outputs

After applying, Terraform will output:

- Kubernetes cluster endpoint
- Database connection strings
- Load balancer IPs
- S3/Storage bucket names
- IAM role ARNs

## Security

- All passwords are managed via AWS Secrets Manager / GCP Secret Manager / Azure Key Vault
- IAM roles use least privilege principle
- Network security groups restrict access
- Encryption at rest and in transit enabled

## Cost Optimization

- Auto-scaling groups for Kubernetes nodes
- Spot instances for non-production workloads
- Lifecycle policies for S3/Storage
- Reserved instances for predictable workloads

## Disaster Recovery

- Automated daily backups for databases
- Multi-AZ/region deployment
- Point-in-time recovery enabled
- Backup retention: 30 days

## Monitoring

Infrastructure includes:

- CloudWatch / Cloud Monitoring / Azure Monitor
- Application Performance Monitoring
- Log aggregation
- Alerting rules

## Clean Up

To destroy infrastructure:

```bash
terraform destroy -var-file="production.tfvars"
```

**Warning:** This will delete all resources including data!
