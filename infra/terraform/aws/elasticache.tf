# ElastiCache Redis Cluster
resource "aws_elasticache_subnet_group" "main" {
  name       = "${local.name_prefix}-redis-subnet-group"
  subnet_ids = module.vpc.private_subnets

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-redis-subnet-group"
    }
  )
}

resource "aws_elasticache_parameter_group" "redis" {
  name   = "${local.name_prefix}-redis7"
  family = "redis7"

  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }

  parameter {
    name  = "timeout"
    value = "300"
  }

  tags = local.common_tags
}

resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "${local.name_prefix}-redis"
  engine               = "redis"
  engine_version       = "7.0"
  node_type            = var.redis_node_type
  num_cache_nodes      = var.redis_num_cache_nodes
  parameter_group_name = aws_elasticache_parameter_group.redis.name
  port                 = 6379

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  # Security - ElastiCache cluster doesn't support these directly, need replication group for encryption
  # at_rest_encryption_enabled = true
  # transit_encryption_enabled = false
  # kms_key_id                 = aws_kms_key.elasticache.arn

  # Backups
  snapshot_retention_limit = 5
  snapshot_window          = "03:00-05:00"
  maintenance_window       = "mon:05:00-mon:07:00"

  # Notifications
  notification_topic_arn = var.enable_monitoring ? aws_sns_topic.elasticache_notifications[0].arn : null

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-redis"
    }
  )
}

# KMS Key for ElastiCache encryption
resource "aws_kms_key" "elasticache" {
  description             = "ElastiCache encryption key"
  deletion_window_in_days = 10
  enable_key_rotation     = true

  tags = merge(
    local.common_tags,
    {
      Name = "${local.name_prefix}-elasticache-key"
    }
  )
}

resource "aws_kms_alias" "elasticache" {
  name          = "alias/${local.name_prefix}-elasticache"
  target_key_id = aws_kms_key.elasticache.key_id
}

# SNS Topic for ElastiCache notifications
resource "aws_sns_topic" "elasticache_notifications" {
  count = var.enable_monitoring ? 1 : 0

  name = "${local.name_prefix}-elasticache-notifications"

  tags = local.common_tags
}

# CloudWatch alarms for ElastiCache
resource "aws_cloudwatch_metric_alarm" "redis_cpu" {
  count = var.enable_monitoring ? 1 : 0

  alarm_name          = "${local.name_prefix}-redis-cpu-utilization"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Average"
  threshold           = 75
  alarm_description   = "Triggers when Redis CPU exceeds 75%"

  dimensions = {
    CacheClusterId = aws_elasticache_cluster.redis.id
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "redis_memory" {
  count = var.enable_monitoring ? 1 : 0

  alarm_name          = "${local.name_prefix}-redis-memory-utilization"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "DatabaseMemoryUsagePercentage"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "Triggers when Redis memory exceeds 80%"

  dimensions = {
    CacheClusterId = aws_elasticache_cluster.redis.id
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "redis_evictions" {
  count = var.enable_monitoring ? 1 : 0

  alarm_name          = "${local.name_prefix}-redis-evictions"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Evictions"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Sum"
  threshold           = 1000
  alarm_description   = "Triggers when Redis evictions exceed 1000"

  dimensions = {
    CacheClusterId = aws_elasticache_cluster.redis.id
  }

  tags = local.common_tags
}
