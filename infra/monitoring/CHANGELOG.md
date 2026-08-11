# Changelog - Monitoring Infrastructure

All notable changes to the monitoring infrastructure are documented here.

## [Unreleased] - V4 Planned

### Planned (V4)

#### Prometheus
- Metrics collection from all services
- Custom metrics for:
  - File upload/download rates
  - Storage usage per user
  - API response times
  - Error rates
  - Cache hit ratios
- Alert rules for:
  - High CPU/memory usage
  - Disk space warnings
  - Service downtime
  - Error rate spikes
- Service discovery for Kubernetes

#### Grafana
- Dashboard for system metrics
- Dashboard for business metrics
- Dashboard for user analytics
- Dashboard for storage analytics
- Alert notifications (Email, Slack)
- Custom visualizations

#### Jaeger (Distributed Tracing)
- Request tracing across services
- Latency analysis
- Dependency graphs
- Performance bottleneck identification

#### EFK Stack (Logging)
- Elasticsearch for log storage
- Fluentd for log collection
- Kibana for log visualization
- Centralized logging from all services
- Log retention policies

#### CloudWatch Integration
- AWS service metrics
- S3 metrics (PUT/GET requests, bandwidth)
- RDS metrics (connections, CPU, IOPS)
- EKS cluster metrics
- Lambda metrics (if used)

### Security (Planned)
- `.gitignore` to protect:
  - Grafana database
  - Prometheus data
  - Secret configurations
  - API keys

### Alerts (Planned)
- **Critical**: Service down, disk full, database unavailable
- **Warning**: High CPU (>80%), high memory (>85%), slow queries
- **Info**: New deployment, scaling event, backup completed

### Structure (Planned)
```
monitoring/
├── prometheus/
│   ├── prometheus.yml
│   ├── alerts/
│   └── rules/
├── grafana/
│   ├── dashboards/
│   ├── provisioning/
│   └── datasources/
├── jaeger/
│   └── jaeger-config.yaml
└── logging/
    ├── fluentd/
    └── kibana/
```

---

## Note

This infrastructure will be implemented in V4 as part of the observability upgrade. Currently, Ziboto relies on application logs and basic Docker container monitoring.
