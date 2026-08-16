# Ziboto CI/CD Infrastructure

This Terraform module creates the IAM infrastructure required for GitHub Actions to automatically deploy the Ziboto backend to AWS.

## What This Module Creates

- **GitHub OIDC Provider**: Allows GitHub Actions to assume AWS IAM roles without long-lived credentials
- **IAM Role**: Specific role for GitHub Actions with least-privilege permissions
- **IAM Policy**: Permissions for deployment operations (S3, Auto Scaling, ELB, CloudWatch)

## Prerequisites

- AWS CLI configured with admin credentials
- Terraform >= 1.5.0
- GitHub repository: `AlliedEdge/ziboto`
- Existing infrastructure (EC2, ASG, S3, etc.) already deployed

## One-Time Setup

### Step 1: Apply Terraform Configuration

```bash
cd infra/terraform/aws/cicd

# Initialize Terraform
terraform init

# Review what will be created
terraform plan

# Create the CI/CD infrastructure
terraform apply
```

This will output the IAM role ARN needed for GitHub Actions.

### Step 2: Configure GitHub Secret

1. Go to your GitHub repository settings:
   ```
   https://github.com/AlliedEdge/ziboto/settings/secrets/actions
   ```

2. Click "New repository secret"

3. Add the secret:
   - **Name**: `AWS_DEPLOYMENT_ROLE_ARN`
   - **Value**: (copy from terraform output `github_actions_role_arn`)

4. Click "Add secret"

### Step 3: Test the Deployment

```bash
# Make a change to backend code
cd apps/backend
# ... make changes ...

# Commit and push
git add .
git commit -m "Test CI/CD deployment"
git push origin main
```

GitHub Actions will automatically:
1. Build and test the backend
2. Upload JAR to S3
3. Trigger rolling deployment
4. Verify health checks

Monitor the deployment:
```
https://github.com/AlliedEdge/ziboto/actions
```

## How It Works

### Deployment Flow

```
Developer
  ↓ git push origin main
GitHub Repository
  ↓ triggers
GitHub Actions Workflow
  ↓ authenticates via OIDC
AWS IAM Role (no keys!)
  ↓ permissions to
S3 + Auto Scaling Group
  ↓ rolling deployment
EC2 Instances (1 at a time)
  ↓ health checks
Application Load Balancer
  ↓ serves traffic
Production (zero downtime)
```

### Security Features

- **No long-lived credentials**: OIDC tokens expire after deployment
- **Least privilege**: Role can only deploy, not manage infrastructure
- **Branch restriction**: Only `main` branch can trigger deployments
- **Repository restriction**: Only `AlliedEdge/ziboto` can use this role
- **Audit trail**: All deployments logged in CloudWatch and GitHub

### Deployment Strategy

**Instance Refresh (Rolling Deployment)**:
- Minimum 50% healthy instances during deployment
- Instances warmed up for 3 minutes (180s)
- Checkpoints at 50% and 100% with 60s delays
- Health checks via ALB target group
- Automatic rollback on failure

### Manual Deployment

Trigger deployment manually without code changes:

1. Go to Actions tab: `https://github.com/AlliedEdge/ziboto/actions`
2. Select "Deploy Backend to AWS" workflow
3. Click "Run workflow"
4. Choose branch: `main`
5. Click "Run workflow"

## Permissions

The GitHub Actions role has permissions to:

✅ **S3**: Upload JAR to `s3://ziboto-files-277522752099-eu-north-1-an/deploy/`  
✅ **Auto Scaling**: Start/cancel instance refresh, describe ASG status  
✅ **EC2**: Describe launch templates (read-only)  
✅ **ELB**: Describe load balancers, target groups, health status  
✅ **CloudWatch Logs**: Read deployment logs  

❌ **Cannot**: Modify infrastructure, delete resources, access other S3 paths, create/delete EC2 instances

## Rollback

If a deployment fails, GitHub Actions will:
1. Automatically cancel the instance refresh
2. Keep existing healthy instances running
3. Report failure in GitHub Actions log

Manual rollback:
```bash
# Cancel ongoing deployment
aws autoscaling cancel-instance-refresh \
  --auto-scaling-group-name ziboto-prod-backend-asg \
  --region eu-north-1

# Upload previous JAR version
aws s3 cp previous-version.jar \
  s3://ziboto-files-277522752099-eu-north-1-an/deploy/ziboto-backend.jar

# Trigger new deployment
# (push to main or manual workflow trigger)
```

## Monitoring

### GitHub Actions
- Workflow runs: `https://github.com/AlliedEdge/ziboto/actions`
- Deployment logs: Click on workflow run → View logs

### AWS
- CloudWatch Logs: `/aws/ec2/ziboto-backend`
- Auto Scaling Activity: AWS Console → EC2 → Auto Scaling Groups → `ziboto-prod-backend-asg` → Activity
- Target Health: AWS Console → EC2 → Load Balancers → Target Groups → `ziboto-prod-backend-tg`

### Health Checks
- ALB Health: `http://ziboto-prod-alb-1310646590.eu-north-1.elb.amazonaws.com:8080/actuator/health`
- Production API: `https://api.ziboto.alliededge.app/actuator/health`

## Troubleshooting

### Deployment Fails: "Access Denied"

**Cause**: GitHub secret not configured or role ARN incorrect

**Solution**:
```bash
# Get the correct role ARN
cd infra/terraform/aws/cicd
terraform output github_actions_role_arn

# Update GitHub secret with this ARN
```

### Deployment Fails: "No healthy instances"

**Cause**: New instances failing health checks

**Solution**:
1. Check CloudWatch logs: `/aws/ec2/ziboto-backend`
2. SSH into instance (if configured): Check systemd service status
3. Verify S3 JAR file: `aws s3 ls s3://ziboto-files-277522752099-eu-north-1-an/deploy/`
4. Check security groups: Port 8080 open to ALB security group
5. Check environment variables: Correct database/Redis/RabbitMQ endpoints

### Deployment Stuck: "Waiting for instance refresh"

**Cause**: Instance refresh not progressing

**Solution**:
```bash
# Check instance refresh status
aws autoscaling describe-instance-refreshes \
  --auto-scaling-group-name ziboto-prod-backend-asg \
  --region eu-north-1

# If stuck, cancel and restart
aws autoscaling cancel-instance-refresh \
  --auto-scaling-group-name ziboto-prod-backend-asg \
  --region eu-north-1
```

### Tests Failing in CI

**Cause**: Missing test dependencies or environment configuration

**Solution**:
1. Run tests locally first: `cd apps/backend && mvn test`
2. Check test logs in GitHub Actions
3. Ensure H2 database is used for tests (not PostgreSQL)
4. Add `skip_tests: true` in workflow_dispatch for emergency hotfix

## Cost

This CI/CD infrastructure has **zero additional cost**:
- GitHub OIDC Provider: Free
- IAM Role/Policy: Free
- GitHub Actions: 2,000 minutes/month free (private repos), unlimited (public repos)

Each deployment uses approximately:
- GitHub Actions: 5-10 minutes
- AWS API calls: < $0.01
- S3 PUT: < $0.01
- Total: < $0.02 per deployment

## Maintenance

### Update Allowed Branch

```hcl
# Edit cicd/main.tf
variable "github_branch" {
  default = "production"  # Change to your branch
}

# Apply
terraform apply
```

### Update Permissions

```hcl
# Edit cicd/main.tf - aws_iam_policy_document
# Add or modify permissions

# Apply
terraform apply
```

### Revoke CI/CD Access

```bash
# Delete the CI/CD infrastructure
terraform destroy

# Remove GitHub secret
# (manually in GitHub UI)
```

## Security Best Practices

✅ **DO**:
- Keep role ARN secret in GitHub Secrets (not in code)
- Review GitHub Actions logs for security issues
- Monitor AWS CloudTrail for unexpected API calls
- Rotate credentials if role ARN is leaked (delete/recreate role)
- Use branch protection rules on `main`

❌ **DON'T**:
- Commit AWS credentials to code
- Share role ARN publicly
- Grant admin permissions to CI/CD role
- Allow deployments from forks
- Disable security scanning in GitHub

## Support

- GitHub Actions Docs: https://docs.github.com/en/actions
- AWS OIDC for GitHub: https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services
- Terraform AWS Provider: https://registry.terraform.io/providers/hashicorp/aws/latest/docs
- Ziboto Issues: https://github.com/AlliedEdge/ziboto/issues
