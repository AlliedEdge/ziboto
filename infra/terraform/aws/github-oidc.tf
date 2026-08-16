# GitHub Actions OIDC Provider and IAM Role for CI/CD
# This file should be applied once to set up the CI/CD infrastructure

# GitHub OIDC Provider (for GitHub Actions to assume AWS IAM roles)
resource "aws_iam_openid_connect_provider" "github_actions" {
  url = "https://token.actions.githubusercontent.com"
  
  client_id_list = [
    "sts.amazonaws.com"
  ]
  
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",  # GitHub Actions OIDC thumbprint
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd"   # Backup thumbprint
  ]
  
  tags = {
    Name        = "${var.project_name}-${var.environment}-github-oidc"
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
    Purpose     = "GitHub Actions OIDC authentication"
  }
}

# IAM Role for GitHub Actions to deploy backend
resource "aws_iam_role" "github_actions_deploy" {
  name               = "${var.project_name}-${var.environment}-github-deploy-role"
  description        = "IAM role for GitHub Actions to deploy Ziboto backend"
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume_role.json
  
  tags = {
    Name        = "${var.project_name}-${var.environment}-github-deploy-role"
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
    Purpose     = "GitHub Actions deployment"
  }
}

# Assume role policy - allows GitHub Actions from specific repo
data "aws_iam_policy_document" "github_actions_assume_role" {
  statement {
    effect = "Allow"
    
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }
    
    actions = ["sts:AssumeRoleWithWebIdentity"]
    
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      # Restrict to specific repository and branch
      values = [
        "repo:AlliedEdge/ziboto:ref:refs/heads/main",
        "repo:AlliedEdge/ziboto:environment:production"
      ]
    }
  }
}

# IAM Policy for GitHub Actions deployment
resource "aws_iam_role_policy" "github_actions_deploy" {
  name   = "${var.project_name}-${var.environment}-github-deploy-policy"
  role   = aws_iam_role.github_actions_deploy.id
  policy = data.aws_iam_policy_document.github_actions_deploy.json
}

data "aws_iam_policy_document" "github_actions_deploy" {
  # S3 permissions - upload JAR to deployment bucket
  statement {
    sid    = "S3DeploymentAccess"
    effect = "Allow"
    
    actions = [
      "s3:PutObject",
      "s3:PutObjectAcl",
      "s3:GetObject",
      "s3:GetObjectVersion",
      "s3:ListBucket"
    ]
    
    resources = [
      data.aws_s3_bucket.storage.arn,
      "${data.aws_s3_bucket.storage.arn}/deploy/*"
    ]
  }
  
  # Auto Scaling Group permissions - trigger instance refresh
  statement {
    sid    = "AutoScalingDeployment"
    effect = "Allow"
    
    actions = [
      "autoscaling:StartInstanceRefresh",
      "autoscaling:CancelInstanceRefresh",
      "autoscaling:DescribeInstanceRefreshes",
      "autoscaling:DescribeAutoScalingGroups",
      "autoscaling:DescribeScalingActivities"
    ]
    
    resources = [
      aws_autoscaling_group.backend.arn
    ]
  }
  
  # EC2 permissions - read launch template info
  statement {
    sid    = "EC2LaunchTemplateRead"
    effect = "Allow"
    
    actions = [
      "ec2:DescribeLaunchTemplates",
      "ec2:DescribeLaunchTemplateVersions"
    ]
    
    resources = ["*"]
  }
  
  # ELB permissions - check target health
  statement {
    sid    = "ELBHealthCheck"
    effect = "Allow"
    
    actions = [
      "elasticloadbalancing:DescribeLoadBalancers",
      "elasticloadbalancing:DescribeTargetGroups",
      "elasticloadbalancing:DescribeTargetHealth",
      "elasticloadbalancing:DescribeListeners"
    ]
    
    resources = ["*"]
  }
  
  # CloudWatch Logs - read deployment logs (optional)
  statement {
    sid    = "CloudWatchLogsRead"
    effect = "Allow"
    
    actions = [
      "logs:DescribeLogGroups",
      "logs:DescribeLogStreams",
      "logs:GetLogEvents",
      "logs:FilterLogEvents"
    ]
    
    resources = [
      "arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:log-group:/aws/ec2/ziboto-backend*"
    ]
  }
}

# Output the IAM role ARN to be used in GitHub Secrets
output "github_actions_role_arn" {
  description = "IAM Role ARN for GitHub Actions deployment (add to GitHub Secrets as AWS_DEPLOYMENT_ROLE_ARN)"
  value       = aws_iam_role.github_actions_deploy.arn
}

output "github_oidc_provider_arn" {
  description = "GitHub OIDC Provider ARN"
  value       = aws_iam_openid_connect_provider.github_actions.arn
}

# Data source for current AWS account ID
data "aws_caller_identity" "current" {}
