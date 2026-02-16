#!/bin/bash
# =============================================================================
# Performance Portal - Ubuntu 22.04 Uninstall Script
# =============================================================================
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

if [[ $EUID -ne 0 ]]; then
    echo -e "${RED}[ERROR]${NC} Run as root: sudo ./uninstall.sh"
    exit 1
fi

echo -e "${YELLOW}This will stop and remove the Performance Portal.${NC}"
read -p "Continue? (y/N): " confirm
[[ "$confirm" != "y" && "$confirm" != "Y" ]] && exit 0

log "Stopping services..."
systemctl stop perf-portal 2>/dev/null || true
systemctl disable perf-portal 2>/dev/null || true
rm -f /etc/systemd/system/perf-portal.service
systemctl daemon-reload

log "Removing Nginx config..."
rm -f /etc/nginx/sites-enabled/perf-portal
rm -f /etc/nginx/sites-available/perf-portal
rm -rf /var/www/perf-portal
systemctl restart nginx 2>/dev/null || true

log "Removing application files..."
rm -rf /opt/performance-portal

log "Removing firewall rules..."
ufw delete allow 3000/tcp 2>/dev/null || true
ufw delete allow 8080/tcp 2>/dev/null || true

warn "PostgreSQL and database were NOT removed (data preservation)."
warn "To drop the database:  sudo -u postgres psql -c 'DROP DATABASE performance_portal;'"
warn "To remove PostgreSQL:  sudo apt-get remove --purge postgresql-15*"

echo -e "${GREEN}Performance Portal uninstalled.${NC}"
