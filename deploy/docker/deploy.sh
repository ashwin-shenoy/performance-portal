#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

cd "${REPO_ROOT}"

if [[ ! -f "frontend/.npmrc" ]]; then
	if [[ -f "frontend/.npmrc.example" ]]; then
		cp "frontend/.npmrc.example" "frontend/.npmrc"
		echo "Created frontend/.npmrc from .npmrc.example (gitignored)."
	else
		echo "Missing frontend/.npmrc and frontend/.npmrc.example. Cannot build frontend image securely."
		exit 1
	fi
fi

if grep -q '\${NPM_TOKEN}' "frontend/.npmrc" && [[ -z "${NPM_TOKEN:-}" ]]; then
	echo "NPM_TOKEN is not set. If private npm packages are required, export NPM_TOKEN before deployment."
fi

docker compose up -d --build

echo "Deployment complete."
