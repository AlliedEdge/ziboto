# CI/CD Module Variables

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "ziboto"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-north-1"
}

variable "s3_bucket_name" {
  description = "S3 bucket name for deployments"
  type        = string
  default     = "ziboto-files-277522752099-eu-north-1-an"
}

variable "github_org" {
  description = "GitHub organization or username"
  type        = string
  default     = "AlliedEdge"
}

variable "github_repo" {
  description = "GitHub repository name"
  type        = string
  default     = "ziboto"
}

variable "github_branch" {
  description = "GitHub branch for deployments"
  type        = string
  default     = "main"
}
