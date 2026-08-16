# Amazon MQ for RabbitMQ
resource "aws_mq_broker" "rabbitmq" {
  broker_name        = "${local.name_prefix}-rabbitmq"
  engine_type        = "RabbitMQ"
  engine_version     = "3.13"
  host_instance_type = "mq.m7g.medium" # Smallest RabbitMQ-supported instance
  deployment_mode    = var.amazonmq_deployment_mode
  security_groups    = [aws_security_group.amazonmq.id]
  subnet_ids         = var.amazonmq_deployment_mode == "SINGLE_INSTANCE" ? [module.vpc.private_subnets[0]] : slice(module.vpc.private_subnets, 0, 2)

  publicly_accessible        = false
  auto_minor_version_upgrade = true

  user {
    username = "ziboto"
    password = var.rabbitmq_password
  }

  logs {
    general = var.enable_monitoring
  }

  encryption_options {
    use_aws_owned_key = false
    kms_key_id        = aws_kms_key.amazonmq.arn
  }

  maintenance_window_start_time {
    day_of_week = "SUNDAY"
    time_of_day = "03:00"
    time_zone   = "UTC"
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-rabbitmq"
    }
  )
}

# KMS Key for Amazon MQ encryption
resource "aws_kms_key" "amazonmq" {
  description             = "KMS key for Amazon MQ encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-amazonmq-key"
    }
  )
}

resource "aws_kms_alias" "amazonmq" {
  name          = "alias/${local.name_prefix}-amazonmq"
  target_key_id = aws_kms_key.amazonmq.key_id
}

# CloudWatch Log Group for Amazon MQ
resource "aws_cloudwatch_log_group" "amazonmq" {
  count = var.enable_monitoring ? 1 : 0

  name              = "/aws/amazonmq/${local.name_prefix}-rabbitmq"
  retention_in_days = var.log_retention_days

  tags = local.common_tags
}
