#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.production.yml"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"
WAIT_SECONDS="${WAIT_SECONDS:-180}"

required_vars=(DOMAIN POSTGRES_DB DB_USERNAME DB_PASSWORD JWT_SECRET DEMO_USERNAME DEMO_PASSWORD)

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # The deployment environment file is local configuration and is never printed.
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

missing=0
for name in "${required_vars[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    missing=1
  fi
done

if (( missing )); then
  echo "Provide the variables in $ENV_FILE or export them before running this script." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose is required." >&2
  exit 1
fi

compose=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

echo "Validating production Compose configuration..."
"${compose[@]}" config --quiet

echo "Building and starting production services..."
"${compose[@]}" up --detach --build --remove-orphans

services=(caddy frontend app postgres kafka redis)
deadline=$((SECONDS + WAIT_SECONDS))

echo "Waiting for service healthchecks..."
while (( SECONDS < deadline )); do
  status="$("${compose[@]}" ps --all --format '{{.Service}}|{{.Health}}|{{.State}}')"
  ready=1

  for service in "${services[@]}"; do
    line="$(printf '%s\n' "$status" | awk -F'|' -v service="$service" '$1 == service { print; exit }')"
    if [[ "$line" != "$service|healthy|running" ]]; then
      ready=0
      break
    fi
  done

  if (( ready )); then
    echo "All production services are healthy."
    "${compose[@]}" ps
    exit 0
  fi

  sleep 3
done

echo "Timed out waiting for production services to become healthy." >&2
"${compose[@]}" ps >&2
exit 1
