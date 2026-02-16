#!/bin/bash
# =============================================================================
# Performance Portal - OpenShift Undeploy Script
# =============================================================================
set -euo pipefail

NAMESPACE="perf-portal"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}This will remove the entire Performance Portal from OpenShift.${NC}"
echo -e "${YELLOW}Namespace '${NAMESPACE}' and ALL data (including database) will be deleted.${NC}"
read -p "Continue? (y/N): " confirm
[[ "$confirm" != "y" && "$confirm" != "Y" ]] && exit 0

echo -e "${GREEN}[INFO]${NC} Deleting namespace '${NAMESPACE}' and all resources..."
oc delete namespace "${NAMESPACE}" --wait=true 2>/dev/null || true

echo -e "${GREEN}Performance Portal removed from OpenShift.${NC}"
