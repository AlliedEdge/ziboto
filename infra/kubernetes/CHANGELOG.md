# Changelog - Kubernetes Infrastructure

All notable changes to the Kubernetes infrastructure are documented here.

## [Unreleased] - V4 Planned

### Planned (V4)
- Kubernetes deployments for all services
- Helm charts for package management
- Horizontal Pod Autoscaler (HPA)
- Ingress with AWS ALB Controller
- ConfigMaps for configuration
- Secrets management
- Service definitions
- Persistent Volume Claims
- Network policies
- Resource limits and requests
- Liveness and readiness probes
- Rolling update strategies
- Namespace isolation

### Services (Planned)
- **Backend**: Spring Boot API (3 replicas)
- **Frontend**: React SPA (2 replicas)
- **PostgreSQL**: StatefulSet with persistent storage
- **Redis**: Master-slave replication
- **RabbitMQ**: Cluster with 3 nodes
- **Nginx**: Ingress controller

### Security (Planned)
- `.gitignore` to protect:
  - Secret YAML files
  - Kubeconfig files
  - Certificate files
  - Environment-specific configs

### Monitoring (Planned)
- Prometheus for metrics
- Grafana for dashboards
- Jaeger for tracing
- EFK stack for logging

### Structure (Planned)
```
kubernetes/
├── base/
│   ├── backend/
│   ├── frontend/
│   ├── database/
│   └── cache/
├── overlays/
│   ├── dev/
│   ├── staging/
│   └── prod/
├── helm/
│   └── ziboto-chart/
└── manifests/
```

---

## Note

This infrastructure will be implemented in V4 as part of the cloud-native upgrade to Amazon EKS. Currently, Ziboto uses Docker Compose for orchestration.
