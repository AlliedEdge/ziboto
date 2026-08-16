# ---------------------------------------------------------------------------
# GitHub Actions OIDC — Dedicated deployment identity for CI/CD
#
# Creates:
#   1. OIDC identity provider for token.actions.githubusercontent.com
#   2. IAM role  : ziboto-github-actions-deploy-role
#   3. Inline policy with minimum permissions required by backend-deploy.yml
#
# Does NOT reuse aws_iam_role.backend (the EC2 instance profile role).
# Does NOT grant AdministratorAccess or any wildcard write permissions.
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
# 1. GitHub Actions OIDC Identity Provider
# ---------------------------------------------------------------------------
resource "aws_iam_openid_connect_provider" "github_actions" {
  url = "https://token.actions.githubusercontent.com"

  # The audience GitHub Actions presents when requesting a token.
  client_id_list = ["sts.amazonaws.com"]

  # GitHub's OIDC thumbprint for token.actions.githubusercontent.com.
  # AWS validates the issuer certificate against its built-in CA store for
  # this well-known URL, but the Terraform resource still requires the list.
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd",
  ]

  tags = merge(
    local.common_tags,
    {
      Name = "github-actions-oidc-provider"
    }
  )
}

# ---------------------------------------------------------------------------
# 2. Dedicated IAM Role for GitHub Actions deployments
# ---------------------------------------------------------------------------
resource "aws_iam_role" "github_actions_deploy" {
  name        = "ziboto-github-actions-deploy-role"
  description = "Assumed by GitHub Actions OIDC tokens to deploy the Ziboto backend (AlliedEdge/ziboto, main branch only)"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "GitHubActionsOIDC"
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.github_actions.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            # Ensures the token was issued for the correct audience.
            "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          }
          StringLike = {
            # Restricts to pushes / workflow_dispatch runs on the main branch
            # of the AlliedEdge/ziboto repository only.
            "token.actions.githubusercontent.com:sub" = "repo:AlliedEdge/ziboto:ref:refs/heads/main"
          }
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "ziboto-github-actions-deploy-role"
    }
  )
}

# ---------------------------------------------------------------------------
# 3. Minimum-privilege inline policy
#
# Covers exactly the AWS API calls made by .github/workflows/backend-deploy.yml:
#
#   Step: "Verify AWS credentials"
#     - sts:GetCallerIdentity  (no explicit policy needed; included for clarity)
#
#   Step: "Upload JAR to S3" + "Verify upload"
#     - s3:PutObject    → deploy/ prefix only
#     - s3:GetObject    → deploy/ prefix only
#     - s3:ListBucket   → bucket, prefix-conditioned to deploy/
#
#   Step: "Start Auto Scaling Group instance refresh"
#     - autoscaling:StartInstanceRefresh → ziboto-prod-backend-asg only
#
#   Step: "Wait for instance refresh to complete"
#     - autoscaling:DescribeInstanceRefreshes → * (Describe APIs are global-only)
#
#   Step: "Verify deployment health"
#     - elasticloadbalancing:DescribeTargetGroups → * (read-only, global-only)
#     - elasticloadbalancing:DescribeTargetHealth → * (read-only, global-only)
#
#   Step: "Test health endpoint"
#     - elasticloadbalancing:DescribeLoadBalancers → * (read-only, global-only)
# ---------------------------------------------------------------------------
resource "aws_iam_role_policy" "github_actions_deploy" {
  name = "ziboto-github-actions-deploy-policy"
  role = aws_iam_role.github_actions_deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # --- S3: upload and verify the backend JAR artifact ---
      {
        Sid    = "S3DeployArtifactReadWrite"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
        ]
        Resource = "${local.s3_bucket_arn}/deploy/*"
      },
      {
        Sid      = "S3ListDeployPrefix"
        Effect   = "Allow"
        Action   = "s3:ListBucket"
        Resource = local.s3_bucket_arn
        Condition = {
          StringLike = {
            "s3:prefix" = "deploy/*"
          }
        }
      },

      # --- Auto Scaling Group: trigger and monitor rolling instance refresh ---
      {
        Sid    = "ASGStartInstanceRefresh"
        Effect = "Allow"
        Action = "autoscaling:StartInstanceRefresh"
        # Resource-level restriction to the specific ASG.
        Resource = "arn:aws:autoscaling:${var.aws_region}:${data.aws_caller_identity.current.account_id}:autoScalingGroup:*:autoScalingGroupName/${aws_autoscaling_group.backend.name}"
      },
      {
        Sid    = "ASGDescribeInstanceRefreshes"
        Effect = "Allow"
        # Describe* auto scaling actions do not support resource-level
        # restrictions — AWS requires Resource = "*" for them.
        Action   = "autoscaling:DescribeInstanceRefreshes"
        Resource = "*"
      },

      # --- ELB/ALB: read-only health and target group lookups ---
      # All elbv2 Describe* actions are global-only (no resource-level support).
      {
        Sid    = "ELBDescribeReadOnly"
        Effect = "Allow"
        Action = [
          "elasticloadbalancing:DescribeLoadBalancers",
          "elasticloadbalancing:DescribeTargetGroups",
          "elasticloadbalancing:DescribeTargetHealth",
        ]
        Resource = "*"
      },
    ]
  })
}

# ---------------------------------------------------------------------------
# Data source: current AWS account ID (used to build resource ARNs above)
# ---------------------------------------------------------------------------
data "aws_caller_identity" "current" {}
