# Production deployment

The production stack is defined in `docker-compose.production.yml`. It runs Caddy, the React/Nginx frontend, Spring Boot, PostgreSQL, Kafka, and Redis on one private Docker network.

## Prerequisites

- A host with Docker Compose and ports 80/443 available
- DNS `A`/`AAAA` records for the deployment domain pointing to the host
- A production `.env` file that is not committed

Required environment variables:

```dotenv
DOMAIN=pulseops.example.com
POSTGRES_DB=pulseops
DB_USERNAME=replace-me
DB_PASSWORD=replace-me
JWT_SECRET=replace-with-a-random-secret-at-least-32-characters
DEMO_USERNAME=replace-with-demo-username
DEMO_PASSWORD=replace-with-demo-password
```

Optional rate-limit settings default to 10 requests per 60 seconds:

```dotenv
RATE_LIMIT_TRANSACTIONS=10
RATE_LIMIT_WINDOW_SECONDS=60
```

## Start and stop

```bash
docker compose --env-file .env -f docker-compose.production.yml up -d --build
docker compose --env-file .env -f docker-compose.production.yml ps
docker compose --env-file .env -f docker-compose.production.yml logs -f caddy app
docker compose --env-file .env -f docker-compose.production.yml down
```

Caddy obtains and renews the HTTPS certificate for `DOMAIN`. Its certificate and configuration state are stored in the `caddy_data` and `caddy_config` volumes.

Only Caddy publishes host ports 80 and 443. PostgreSQL, Kafka, Redis, the Spring Boot API, and the frontend Nginx server remain private to the Compose network.

## Routing

Caddy forwards the public domain to the frontend Nginx container. Nginx serves the React application and forwards `/api/` to the Spring Boot app. This preserves REST API paths and the `/api/v1/transactions/{id}/status/stream` SSE endpoint; Nginx disables response buffering for the API proxy.

The backend runs with the `production` Spring profile, so Flyway owns schema changes and Hibernate validates the existing schema.

## Validation

```bash
docker compose --env-file .env.example -f docker-compose.production.yml config --quiet
docker compose --env-file .env.example -f docker-compose.yml config --quiet
```

The development Compose file remains separate and retains its host-facing development ports.
