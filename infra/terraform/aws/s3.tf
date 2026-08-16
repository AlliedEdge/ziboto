# S3 Bucket for file storage (existing bucket - using data source)
data "aws_s3_bucket" "storage" {
  bucket = var.s3_bucket_name
}

# For terraform references, create a local resource wrapper
locals {
  s3_bucket_id  = data.aws_s3_bucket.storage.id
  s3_bucket_arn = data.aws_s3_bucket.storage.arn
}

# Block public access
resource "aws_s3_bucket_public_access_block" "storage" {
  bucket = local.s3_bucket_id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Enable versioning
resource "aws_s3_bucket_versioning" "storage" {
  bucket = local.s3_bucket_id

  versioning_configuration {
    status = var.s3_enable_versioning ? "Enabled" : "Disabled"
  }
}

# Server-side encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "storage" {
  bucket = local.s3_bucket_id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.s3.arn
    }
    bucket_key_enabled = true
  }
}

# Lifecycle policy
resource "aws_s3_bucket_lifecycle_configuration" "storage" {
  bucket = local.s3_bucket_id

  rule {
    id     = "intelligent-tiering"
    status = "Enabled"

    filter {}

    transition {
      days          = var.s3_lifecycle_days
      storage_class = "INTELLIGENT_TIERING"
    }

    noncurrent_version_transition {
      noncurrent_days = 30
      storage_class   = "STANDARD_IA"
    }

    noncurrent_version_transition {
      noncurrent_days = 90
      storage_class   = "GLACIER"
    }

    noncurrent_version_expiration {
      noncurrent_days = 365
    }
  }

  rule {
    id     = "abort-incomplete-multipart-uploads"
    status = "Enabled"

    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# CORS configuration
resource "aws_s3_bucket_cors_configuration" "storage" {
  bucket = local.s3_bucket_id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD", "PUT", "POST", "DELETE"]
    allowed_origins = var.domain_name != "" ? ["https://${var.domain_name}", "https://www.${var.domain_name}"] : ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# Bucket logging
resource "aws_s3_bucket_logging" "storage" {
  count = var.enable_monitoring ? 1 : 0

  bucket = local.s3_bucket_id

  target_bucket = aws_s3_bucket.logs[0].id
  target_prefix = "s3-access-logs/"
}

# S3 bucket for logs
resource "aws_s3_bucket" "logs" {
  count = var.enable_monitoring ? 1 : 0

  bucket = "${var.s3_bucket_name}-logs"

  tags = merge(
    local.common_tags,
    {
      Name = "${var.s3_bucket_name}-logs"
    }
  )
}

resource "aws_s3_bucket_public_access_block" "logs" {
  count = var.enable_monitoring ? 1 : 0

  bucket = aws_s3_bucket.logs[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "logs" {
  count = var.enable_monitoring ? 1 : 0

  bucket = aws_s3_bucket.logs[0].id

  rule {
    id     = "delete-old-logs"
    status = "Enabled"

    filter {}

    expiration {
      days = var.log_retention_days
    }
  }
}

# KMS Key for S3 encryption
resource "aws_kms_key" "s3" {
  description             = "S3 bucket encryption key"
  deletion_window_in_days = 10
  enable_key_rotation     = true

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-s3-key"
    }
  )
}

resource "aws_kms_alias" "s3" {
  name          = "alias/${local.name_prefix}-s3"
  target_key_id = aws_kms_key.s3.key_id
}

# CloudWatch metrics for S3
resource "aws_cloudwatch_metric_alarm" "s3_4xx_errors" {
  count = var.enable_monitoring ? 1 : 0

  alarm_name          = "${local.name_prefix}-s3-4xx-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "4xxErrors"
  namespace           = "AWS/S3"
  period              = "300"
  statistic           = "Sum"
  threshold           = "100"
  alarm_description   = "This metric monitors S3 4xx errors"
  alarm_actions       = []

  dimensions = {
    BucketName = local.s3_bucket_id
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "s3_5xx_errors" {
  count = var.enable_monitoring ? 1 : 0

  alarm_name          = "${local.name_prefix}-s3-5xx-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "5xxErrors"
  namespace           = "AWS/S3"
  period              = "300"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "This metric monitors S3 5xx errors"
  alarm_actions       = []

  dimensions = {
    BucketName = local.s3_bucket_id
  }

  tags = local.common_tags
}
