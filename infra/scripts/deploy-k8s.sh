#!/bin/bash

# Ziboto Kubernetes Deployment Script
# This script deploys the Ziboto application to a Kubernetes cluster

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT="${1:-production}"
NAMESPACE="ziboto"

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}Ziboto Kubernetes Deployment${NC}"
echo -e "${GREEN}Environment: ${ENVIRONMENT}${NC}"
echo -e "${GREEN}=====================================${NC}"

# Check prerequisites
echo -e "\n${YELLOW}Checking prerequisites...${NC}"

if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}kubectl is not installed. Please install it first.${NC}"
    exit 1
fi

if ! command -v kustomize &> /dev/null; then
    echo -e "${YELLOW}kustomize is not installed. Using kubectl's built-in kustomize support.${NC}"
    KUSTOMIZE_CMD="kubectl apply -k"
else
    KUSTOMIZE_CMD="kustomize build | kubectl apply -f -"
fi

# Check cluster connectivity
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Cannot connect to Kubernetes cluster. Please check your kubeconfig.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites check passed${NC}"

# Create namespace
echo -e "\n${YELLOW}Creating namespace...${NC}"
kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
echo -e "${GREEN}✓ Namespace created/verified${NC}"

# Create secrets from environment file
echo -e "\n${YELLOW}Creating secrets...${NC}"
if [ -f "../../.env" ]; then
    kubectl create secret generic ziboto-secrets \
        --from-env-file=../../.env \
        -n ${NAMESPACE} \
        --dry-run=client -o yaml | kubectl apply -f -
    echo -e "${GREEN}✓ Secrets created${NC}"
else
    echo -e "${RED}Warning: .env file not found. Please create secrets manually.${NC}"
fi

# Deploy base configurations
echo -e "\n${YELLOW}Deploying base configurations...${NC}"
kubectl apply -f ../kubernetes/base/
echo -e "${GREEN}✓ Base configurations deployed${NC}"

# Deploy stateful services (databases)
echo -e "\n${YELLOW}Deploying stateful services...${NC}"
kubectl apply -f ../kubernetes/statefulsets/
echo -e "${GREEN}✓ Stateful services deployed${NC}"

# Wait for stateful services to be ready
echo -e "\n${YELLOW}Waiting for stateful services to be ready...${NC}"
kubectl wait --for=condition=ready pod -l app=postgres -n ${NAMESPACE} --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n ${NAMESPACE} --timeout=300s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n ${NAMESPACE} --timeout=300s
echo -e "${GREEN}✓ Stateful services are ready${NC}"

# Deploy application services
echo -e "\n${YELLOW}Deploying application services...${NC}"
kubectl apply -f ../kubernetes/services/
echo -e "${GREEN}✓ Application services deployed${NC}"

# Deploy applications
echo -e "\n${YELLOW}Deploying applications...${NC}"
kubectl apply -f ../kubernetes/deployments/
echo -e "${GREEN}✓ Applications deployed${NC}"

# Wait for applications to be ready
echo -e "\n${YELLOW}Waiting for applications to be ready...${NC}"
kubectl wait --for=condition=available deployment -l tier=api -n ${NAMESPACE} --timeout=600s
kubectl wait --for=condition=available deployment -l tier=web -n ${NAMESPACE} --timeout=600s
echo -e "${GREEN}✓ Applications are ready${NC}"

# Deploy autoscaling
echo -e "\n${YELLOW}Deploying autoscaling...${NC}"
kubectl apply -f ../kubernetes/autoscaling/
echo -e "${GREEN}✓ Autoscaling configured${NC}"

# Deploy ingress
echo -e "\n${YELLOW}Deploying ingress...${NC}"
kubectl apply -f ../kubernetes/ingress/
echo -e "${GREEN}✓ Ingress configured${NC}"

# Show deployment status
echo -e "\n${YELLOW}Deployment Status:${NC}"
kubectl get all -n ${NAMESPACE}

# Show ingress information
echo -e "\n${YELLOW}Ingress Information:${NC}"
kubectl get ingress -n ${NAMESPACE}

# Get LoadBalancer IP/Hostname
echo -e "\n${YELLOW}Waiting for LoadBalancer IP...${NC}"
kubectl get ingress ziboto-ingress -n ${NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || \
kubectl get ingress ziboto-ingress -n ${NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || \
echo -e "${YELLOW}LoadBalancer IP not yet assigned. Please check back in a few minutes.${NC}"

echo -e "\n${GREEN}=====================================${NC}"
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo -e "${GREEN}=====================================${NC}"
echo -e "\nTo view logs:"
echo -e "  kubectl logs -f deployment/backend -n ${NAMESPACE}"
echo -e "\nTo access the application:"
echo -e "  kubectl port-forward -n ${NAMESPACE} svc/frontend 8080:80"
echo -e "\nTo scale the backend:"
echo -e "  kubectl scale deployment backend -n ${NAMESPACE} --replicas=5"
