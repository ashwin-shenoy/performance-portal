#!/bin/bash
# =============================================================================
# Performance Portal - OpenShift Cluster Deployment Script (From Scratch)
# =============================================================================
# This script deploys the Performance Portal on an OpenShift cluster.
#
# Prerequisites:
#   - oc CLI installed and logged into the cluster (oc login ...)
#   - Cluster-admin or project-admin permissions
#   - Access to the internal image registry
#
# Usage:
#   chmod +x deploy.sh
#   ./deploy.sh
#
# =============================================================================

set -euo pipefail

# --- Configuration -----------------------------------------------------------
NAMESPACE="perf-portal"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()   { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# --- Pre-flight checks -------------------------------------------------------
if ! command -v oc &>/dev/null; then
    error "'oc' CLI not found. Install it from https://mirror.openshift.com/pub/openshift-v4/clients/oc/"
fi

if ! oc whoami &>/dev/null; then
    error "Not logged in to OpenShift. Run: oc login <cluster-url>"
fi

CLUSTER_URL=$(oc whoami --show-server)
CURRENT_USER=$(oc whoami)
log "Deploying to cluster: ${CLUSTER_URL}"
log "Logged in as: ${CURRENT_USER}"

if [[ ! -d "${SOURCE_ROOT}/backend" || ! -d "${SOURCE_ROOT}/frontend" ]]; then
    error "Source code not found. Run this script from within the project: deploy/openshift/deploy.sh"
fi

# --- Step 1: Create Namespace ------------------------------------------------
log "Step 1/7: Creating namespace '${NAMESPACE}'..."
oc apply -f "${SCRIPT_DIR}/00-namespace.yaml"
oc project "${NAMESPACE}"
log "Namespace ready."

# --- Step 2: Create Secrets ---------------------------------------------------
log "Step 2/7: Creating secrets..."
oc apply -f "${SCRIPT_DIR}/01-secrets.yaml"
log "Secrets created."

# --- Step 3: Deploy PostgreSQL ------------------------------------------------
log "Step 3/7: Deploying PostgreSQL..."
oc apply -f "${SCRIPT_DIR}/02-postgres.yaml"

log "Waiting for PostgreSQL to be ready..."
oc rollout status deployment/postgres -n "${NAMESPACE}" --timeout=120s
log "PostgreSQL is running."

# --- Step 4: Build & Push Images ----------------------------------------------
log "Step 4/7: Building container images..."

# Create ImageStreams and BuildConfigs
oc apply -f "${SCRIPT_DIR}/06-buildconfigs.yaml"

# Build backend image
log "Building backend image (this may take 2-5 minutes)..."
oc start-build backend \
    --from-dir="${SOURCE_ROOT}/backend" \
    --follow \
    --wait \
    -n "${NAMESPACE}"
log "Backend image built."

# Build frontend image
# For OpenShift, the frontend Dockerfile nginx needs to listen on 8080 (non-root)
# Create a temp nginx.conf that listens on 8080 instead of 80
FRONTEND_TMP=$(mktemp -d)
cp -r "${SOURCE_ROOT}/frontend/"* "${FRONTEND_TMP}/"
cat > "${FRONTEND_TMP}/nginx.conf" <<'NGINX_EOF'
server {
    listen 8080;
    server_name _;
    client_max_body_size 200M;

    root /usr/share/nginx/html;
    index index.html;

    location /api/v1/ {
        proxy_pass http://backend:8080/api/v1/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
NGINX_EOF

log "Building frontend image..."
oc start-build frontend \
    --from-dir="${FRONTEND_TMP}" \
    --follow \
    --wait \
    -n "${NAMESPACE}"
rm -rf "${FRONTEND_TMP}"
log "Frontend image built."

# --- Step 5: Deploy Backend ---------------------------------------------------
log "Step 5/7: Deploying backend..."
oc apply -f "${SCRIPT_DIR}/03-backend.yaml"

log "Waiting for backend to be ready (this may take 1-2 minutes)..."
oc rollout status deployment/backend -n "${NAMESPACE}" --timeout=300s
log "Backend is running."

# --- Step 6: Deploy Frontend --------------------------------------------------
log "Step 6/7: Deploying frontend..."
oc apply -f "${SCRIPT_DIR}/04-frontend.yaml"

log "Waiting for frontend to be ready..."
oc rollout status deployment/frontend -n "${NAMESPACE}" --timeout=120s
log "Frontend is running."

# --- Step 7: Create Route -----------------------------------------------------
log "Step 7/7: Creating route..."
oc apply -f "${SCRIPT_DIR}/05-route.yaml"

# Get the route URL
ROUTE_URL=$(oc get route perf-portal -n "${NAMESPACE}" -o jsonpath='{.spec.host}' 2>/dev/null || echo "pending")

# --- Done ---------------------------------------------------------------------
echo ""
echo "============================================================================="
echo -e "${GREEN}  Performance Portal deployed successfully on OpenShift!${NC}"
echo "============================================================================="
echo ""
echo "  Application URL: https://${ROUTE_URL}"
echo ""
echo "  Pods:"
oc get pods -n "${NAMESPACE}" --no-headers 2>/dev/null | sed 's/^/    /'
echo ""
echo "  Manage:"
echo "    oc get pods -n ${NAMESPACE}              # List pods"
echo "    oc logs -f deploy/backend -n ${NAMESPACE} # Backend logs"
echo "    oc logs -f deploy/frontend -n ${NAMESPACE} # Frontend logs"
echo "    oc get route -n ${NAMESPACE}              # Route URL"
echo ""
echo "  Rebuild after code changes:"
echo "    oc start-build backend --from-dir=./backend --follow -n ${NAMESPACE}"
echo "    oc start-build frontend --from-dir=./frontend --follow -n ${NAMESPACE}"
echo "============================================================================="
