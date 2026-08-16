# Ziboto Infrastructure

Production-grade cloud-native infrastructure for Ziboto platform.

## Overview

This directory contains all infrastructure-as-code and deployment configurations for running Ziboto on Kubernetes with AWS, GCP, or Azure.

## Contents

- **[kubernetes/](kubernetes/)** - Kubernetes manifests for container orchestration
- **[terraform/](terraform/)** - Infrastructure-as-code for cloud resources
- **[docker/](docker/)** - Docker Compose configurations for local development
- **[scripts/](scripts/)** - Automation scripts for deployment

## Documentation

- 📖 **[Phase 4 Complete Guide](PHASE4-CLOUD-NATIVE.md)** - Comprehensive documentation
- 🚀 **[Quick Start Guide](QUICK-START.md)** - Get running in 15 minutes
- ☸️ **[Kubernetes README](kubernetes/README.md)** - K8s manifests documentation
- 🏗️ **[Terraform README](terraform/README.md)** - IaC documentation

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Internet Gateway                       │
└────────────────────┬────────────────────────────────────┘
                     │
              ┌──────▼───────┐
              │ Load Balancer │
              │   (AWS ALB)   │
              └──────┬────────┘
                     │
          ┌──────────┴──────────┐
          │                     │
    ┌─────▼─────┐         ┌────▼──────┐
    │ Frontend  │         │  Backend  │
    │  Pods     │         │   Pods    │
    │ (2-5x)    │         │  (3-10x)  │
    └───────────┘         └─────┬─────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
              ┌─────▼────┐ ┌───▼────┐ ┌───▼──────┐
              │PostgreSQL│ │ Redis  │ │ RabbitMQ │
              │   RDS    │ │ElastiC.│ │   MQ     │
              └──────────┘ └────────┘ └──────────┘
```

## Key Features

### ✅ Kubernetes Orchestration
- Container orchestration and lifecycle management
- Auto-scaling based on CPU and memory metrics
- Rolling updates with zero downtime
- Self-healing with automatic pod restarts
- Resource quotas and limits

### ✅ Infrastructure as Code
- Terraform for AWS, GCP, Azure
- Version-controlled infrastructure
- Reproducible environments
- Multi-environment support (dev, staging, prod)

### ✅ High Availability
- Multi-AZ deployment across 3 availability zones
- Database replication with automatic failover
- Redis cluster with automatic failover
- Load balancing across multiple pods
- 99.95% uptime SLA

### ✅ Security
- VPC isolation with private subnets
- Encryption at rest (KMS) for all data stores
- Encryption in transit (TLS/SSL)
- IAM roles for service accounts (no credentials)
- Secrets management with AWS Secrets Manager
- Security groups and network policies
- Non-root containers

### ✅ Auto-Scaling
- Horizontal Pod Autoscaler (HPA)
- Cluster Autoscaler for nodes
- Backend: 3-10 pods based on load
- Frontend: 2-5 pods based on load
- EKS nodes: 2-10 based on demand

### ✅ Monitoring & Observability
- Prometheus for metrics collection
- Grafana for visualization
- CloudWatch for AWS resources
- Centralized logging
- Distributed tracing (future)
- Real-time alerts

### ✅ Disaster Recovery
- Automated daily backups (30-day retention)
- Point-in-time recovery
- Cross-region replication
- RTO < 1 hour, RPO < 15 minutes

## Quick Start

### Option 1: AWS EKS (Production)

```bash
# 1. Provision infrastructure
cd scripts
./setup-aws-infrastructure.sh production us-east-1

# 2. Deploy application
./deploy-k8s.sh production
```

### Option 2: Local Development

```bash
# Using Docker Compose
cd docker
docker-compose up -d

# Or using Minikube
minikube start
kubectl apply -k kubernetes/overlays/dev/
```

## Cloud Providers

### ✅ AWS (Complete)
- EKS for Kubernetes
- RDS for PostgreSQL
- ElastiCache for Redis
- S3 for object storage
- IAM for access management
- CloudWatch for monitoring

### 🚧 GCP (Planned)
- GKE for Kubernetes
- Cloud SQL for PostgreSQL
- Memorystore for Redis
- Cloud Storage for objects
- Cloud IAM
- Cloud Monitoring

### 🚧 Azure (Planned)
- AKS for Kubernetes
- Azure Database for PostgreSQL
- Azure Cache for Redis
- Blob Storage
- Azure AD
- Azure Monitor

## Environments

| Environment | Nodes | Backend Pods | Frontend Pods | Cost/Month |
|-------------|-------|--------------|---------------|------------|
| Development | 1-2   | 1-2          | 1             | $100-150   |
| Staging     | 2-3   | 2-3          | 1-2           | $300-400   |
| Production  | 3-10  | 3-10         | 2-5           | $600-700   |

## Technology Stack

### Orchestration
- **Kubernetes 1.28** - Container orchestration
- **Kustomize** - Configuration management
- **Helm** - Package management (optional)

### Infrastructure
- **Terraform 1.5+** - Infrastructure as code
- **AWS EKS** - Managed Kubernetes
- **AWS RDS** - Managed PostgreSQL
- **AWS ElastiCache** - Managed Redis

### Networking
- **NGINX Ingress Controller** - Load balancing
- **Cert-Manager** - SSL/TLS certificates
- **AWS VPC** - Network isolation

### Monitoring
- **Prometheus** - Metrics collection
- **Grafana** - Visualization
- **CloudWatch** - AWS monitoring
- **AlertManager** - Alerting

### Security
- **AWS KMS** - Encryption keys
- **AWS Secrets Manager** - Secret storage
- **IAM Roles for Service Accounts** - Pod authentication
- **Security Groups** - Network firewalls

## Directory Structure

```
infra/
├── kubernetes/              # Kubernetes manifests
│   ├── base/               # Base configurations
│   ├── deployments/        # Application deployments
│   ├── services/           # Service definitions
│   ├── statefulsets/       # Stateful applications
│   ├── ingress/            # Ingress rules
│   ├── autoscaling/        # HPA configs
│   ├── storage/            # Storage classes
│   └── overlays/           # Environment overlays
│       ├── dev/
│       ├── staging/
│       └── production/
├── terraform/              # Infrastructure as code
│   ├── aws/               # AWS resources
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── vpc.tf
│   │   ├── eks.tf
│   │   ├── rds.tf
│   │   ├── elasticache.tf
│   │   ├── s3.tf
│   │   └── iam.tf
│   ├── gcp/               # GCP resources (future)
│   └── azure/             # Azure resources (future)
├── docker/                 # Docker Compose
│   └── docker-compose.yml
├── scripts/                # Automation
│   ├── setup-aws-infrastructure.sh
│   ├── deploy-k8s.sh
│   └── teardown.sh
├── PHASE4-CLOUD-NATIVE.md  # Complete documentation
├── QUICK-START.md          # 15-minute guide
└── README.md               # This file
```

## Operations

### Deploy
```bash
./scripts/deploy-k8s.sh production
```

### Scale
```bash
kubectl scale deployment backend -n ziboto --replicas=5
```

### Update
```bash
kubectl set image deployment/backend backend=ziboto/backend:v2.0.0 -n ziboto
```

### Monitor
```bash
kubectl top pods -n ziboto
kubectl logs -f deployment/backend -n ziboto
```

### Backup
```bash
aws rds create-db-snapshot \
  --db-instance-identifier ziboto-production-postgres \
  --db-snapshot-identifier backup-$(date +%Y%m%d)
```

### Rollback
```bash
kubectl rollout undo deployment/backend -n ziboto
```

## Cost Management

### Optimization Strategies
1. Use Spot Instances for dev/staging (40-60% savings)
2. Reserved Instances for production (40-60% savings)
3. S3 Intelligent Tiering for automatic optimization
4. Cluster Autoscaler to scale down during low traffic
5. Right-size instances based on actual usage

### Cost Breakdown (Production)
- EKS Control Plane: $73/month
- EC2 Worker Nodes: $190/month (3x t3.large)
- RDS PostgreSQL: $130/month (db.t3.medium Multi-AZ)
- ElastiCache Redis: $120/month (2x cache.t3.medium)
- S3 Storage: $23/month (1TB)
- Data Transfer: $50/month
- CloudWatch: $20/month
- **Total: ~$600-700/month**

## Security Best Practices

✅ Enable encryption at rest for all services
✅ Use IAM roles, not access keys
✅ Apply least privilege principle
✅ Enable MFA for admin access
✅ Regular security updates
✅ Network isolation with security groups
✅ Secrets in Secrets Manager, not in code
✅ Regular security audits
✅ Enable CloudTrail for audit logging
✅ Use private subnets for applications

## Disaster Recovery

### Backup Strategy
- **RDS**: Automated daily backups, 30-day retention
- **S3**: Versioning enabled, lifecycle policies
- **Configuration**: Git repository (infrastructure as code)

### Recovery Procedure
1. Restore RDS from snapshot (15 minutes)
2. Deploy application from Git (10 minutes)
3. Update DNS if needed (5 minutes)
4. Verify functionality (10 minutes)

**Total Recovery Time**: ~40 minutes

## Troubleshooting

### Common Issues

**Pods not starting**
```bash
kubectl describe pod <pod-name> -n ziboto
kubectl logs <pod-name> -n ziboto
```

**Database connection failed**
```bash
kubectl exec -it deployment/backend -n ziboto -- bash
env | grep DATABASE
```

**Out of resources**
```bash
kubectl top nodes
kubectl describe nodes
```

**Image pull errors**
```bash
kubectl describe pod <pod-name> -n ziboto
# Check image name and ECR permissions
```

## CI/CD Integration

### GitHub Actions
```yaml
name: Deploy to EKS
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: aws-actions/configure-aws-credentials@v1
      - run: kubectl apply -k infra/kubernetes/overlays/production/
```

### GitLab CI
```yaml
deploy:
  stage: deploy
  script:
    - kubectl apply -k infra/kubernetes/overlays/production/
  only:
    - main
```

## Support & Contributing

- 📖 **Documentation**: See PHASE4-CLOUD-NATIVE.md
- 🐛 **Bug Reports**: GitHub Issues
- 💡 **Feature Requests**: GitHub Discussions
- 🤝 **Contributing**: See CONTRIBUTING.md

## License

Apache 2.0

## Credits

Built with ❤️ by the Ziboto team using:
- Kubernetes
- Terraform
- AWS
- Spring Boot
- React

---

**Need help?** Check the [Complete Documentation](PHASE4-CLOUD-NATIVE.md) or [Quick Start Guide](QUICK-START.md)
