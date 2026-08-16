# CI/CD Module Outputs

output "github_actions_role_arn" {
  description = "IAM Role ARN for GitHub Actions (add to GitHub Secrets as AWS_DEPLOYMENT_ROLE_ARN)"
  value       = aws_iam_role.github_actions_deploy.arn
}

output "github_actions_role_name" {
  description = "IAM Role name for GitHub Actions"
  value       = aws_iam_role.github_actions_deploy.name
}

output "github_oidc_provider_arn" {
  description = "GitHub OIDC Provider ARN"
  value       = aws_iam_openid_connect_provider.github_actions.arn
}

output "setup_instructions" {
  description = "Instructions for completing CI/CD setup"
  value = <<-EOT
    
    ========================================
    CI/CD SETUP COMPLETE
    ========================================
    
    Next steps to enable GitHub Actions deployments:
    
    1. Add the following secret to your GitHub repository:
       
       Repository: ${var.github_org}/${var.github_repo}
       Secret Name: AWS_DEPLOYMENT_ROLE_ARN
       Secret Value: ${aws_iam_role.github_actions_deploy.arn}
       
       Go to: https://github.com/${var.github_org}/${var.github_repo}/settings/secrets/actions
       Click "New repository secret"
       Name: AWS_DEPLOYMENT_ROLE_ARN
       Value: ${aws_iam_role.github_actions_deploy.arn}
    
    2. Push to main branch to trigger deployment:
       
       git add .
       git commit -m "Enable CI/CD"
       git push origin main
    
    3. Monitor deployment:
       
       https://github.com/${var.github_org}/${var.github_repo}/actions
    
    ========================================
    DEPLOYMENT WORKFLOW
    ========================================
    
    After setup, every push to '${var.github_branch}' branch will:
    1. Build and test backend
    2. Upload JAR to S3
    3. Trigger rolling deployment (instance refresh)
    4. Verify health checks
    5. Complete with zero downtime
    
    Manual deployment:
    - Go to Actions tab in GitHub
    - Select "Deploy Backend to AWS"
    - Click "Run workflow"
    
    ========================================
  EOT
}
