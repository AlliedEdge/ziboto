# Changelog - Terraform Infrastructure

All notable changes to the Terraform infrastructure are documented here.

## [Unreleased] - V4 Planned

### Planned (V4)
- VPC module with public/private subnets
- Amazon EKS cluster configuration
- RDS PostgreSQL with multi-AZ
- ElastiCache Redis cluster
- S3 buckets with lifecycle policies
- IAM roles and policies
- Security groups
- Application Load Balancer
- Route53 DNS configuration
- CloudWatch logging
- Auto-scaling groups
- Backup policies

### Security (Planned)
- `.gitignore` to protect:
  - Terraform state files (`.tfstate`)
  - Variable files with secrets (`.tfvars`)
  - SSH keys and certificates
  - Sensitive outputs

### Structure (Planned)
```
terraform/
├── modules/
│   ├── vpc/
│   ├── eks/
│   ├── rds/
│   ├── elasticache/
│   ├── s3/
│   └── iam/
├── environments/
│   ├── dev/
│   ├── staging/
│   └── prod/
├── main.tf
├── variables.tf
└── outputs.tf
```

---

## Note

This infrastructure will be implemented in V4 as part of the cloud-native upgrade. Currently, Ziboto uses Docker Compose for local development and simple EC2 deployment.
