# PulseOps

PulseOps is a full-stack transaction operations platform: a Spring Boot API, event-driven transaction processing, and a responsive React operations dashboard.

## Architecture

```text
React + Vite + Tailwind
          │  /api via Nginx
          ▼
Spring Boot transaction-service
     │             │              │
     ▼             ▼              ▼
PostgreSQL      Kafka           Redis
 persistence    events          idempotency + rate limiting
```

Creating a transaction persists it in PostgreSQL with status `RECEIVED`, then publishes a JSON event to Kafka. The consumer records `VALIDATING`, `PROCESSING`, and `COMPLETED`; processing failures are logged and rethrown rather than persisted as a `FAILED` transition. Redis provides idempotency and rate limiting. The detail page receives live status changes through Server-Sent Events.

## Features

- JWT-protected transaction APIs
- Create, retrieve, paginate, search, and filter transactions
- Transaction status history and live SSE updates
- Kafka `transaction.created` JSON events and consumer processing
- Redis-backed `Idempotency-Key` support and creation rate limiting
- PostgreSQL persistence with Flyway support
- Actuator health, metrics, and Prometheus endpoints
- Responsive React dashboard with charts, loading/retry/empty states, mobile navigation, error boundary, and 404 page
- Docker Compose stack and GitHub Actions CI

## Stack

**Backend:** Java 21, Spring Boot 4, Spring MVC, Spring Security JWT resource server, Spring Data JPA/Redis, Spring Kafka, Validation, Actuator, PostgreSQL 16, Apache Kafka 3.9, Redis 7, Flyway, Maven Wrapper.

**Frontend:** React, TypeScript, Vite, Tailwind CSS, React Router, and Nginx for the production container.

## Local setup

Requirements: Java 21, Docker Desktop with Compose, and Node.js 22/npm for frontend-only development.

```bash
cp .env.example .env
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3001 |
| Backend | http://localhost:8080 |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5434 |
| Kafka | localhost:9092 |
| Redis | localhost:6379 |

Compose defaults are local-development values. Replace `DB_PASSWORD` and `JWT_SECRET` for shared environments. `.env` files are ignored by Git; only example templates are committed.

To run the frontend separately:

```bash
cd frontend
npm ci
npm run dev
```

Vite proxies `/api` to `http://localhost:8080` by default. Set `VITE_API_PROXY_TARGET` in `frontend/.env` to override that development target.

## API flow

All `/api/v1/**` routes require a JWT bearer token. This application does not issue tokens; use a token from the configured identity provider or a suitable development token. `/actuator/health` is public.

Available transaction endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/v1/transactions` | Create a transaction |
| `GET` | `/api/v1/transactions` | List, paginate, and filter transactions |
| `GET` | `/api/v1/transactions/{id}` | Read one transaction |
| `GET` | `/api/v1/transactions/{id}/status` | Read status history |
| `GET` | `/api/v1/transactions/{id}/status/stream` | Stream status changes with SSE |

### Create

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-123" \
  -d '{"amount":42.50,"currency":"USD","description":"Example transaction"}'
```

The amount must be positive and currency must be a three-letter code. Reusing an idempotency key prevents duplicate creation. Excess creation requests return `429 Too Many Requests`.

### Read and monitor

```bash
curl "http://localhost:8080/api/v1/transactions?page=0&size=20&status=COMPLETED" -H "Authorization: Bearer $JWT"
curl http://localhost:8080/api/v1/transactions/$ID -H "Authorization: Bearer $JWT"
curl http://localhost:8080/api/v1/transactions/$ID/status -H "Authorization: Bearer $JWT"
curl -N http://localhost:8080/api/v1/transactions/$ID/status/stream -H "Authorization: Bearer $JWT"
```

Supported statuses: `RECEIVED`, `VALIDATING`, `PROCESSING`, `COMPLETED`, and `FAILED`.

## Kafka

The producer publishes `TransactionCreatedEvent` JSON messages to `transaction.created`. Events contain transaction ID, amount, currency, status, and creation timestamp. The `transaction-service` consumer logs received events and updates transaction status through the processing lifecycle. Docker clients use `kafka:9092`; host clients use `localhost:9092`.

## Redis

Redis stores idempotency reservations/results and rate-limit counters. The default transaction-creation limit is 10 requests per 60 seconds. Configure it with `RATE_LIMIT_TRANSACTIONS` and `RATE_LIMIT_WINDOW_SECONDS`.

## Security and configuration

- `/api/v1/**` is authenticated with JWT bearer tokens.
- `/actuator/health` is public.
- JWT, database, Kafka, Redis, rate-limit, and server settings are environment-configurable.
- Use `JWT_SECRET`, `DB_USERNAME`, and `DB_PASSWORD` for credentials.
- Never commit `.env` files, private keys, access tokens, or production credentials.

See [.env.example](.env.example) and [frontend/.env.example](frontend/.env.example).

## Testing and builds

Backend tests, Testcontainers integration tests, and packaging:

```bash
./mvnw clean verify
```

This covers unit tests, MockMvc controller tests, and PostgreSQL/Kafka/Redis integration tests. The load test is opt-in and uses an isolated Testcontainers-backed Spring context:

```bash
./mvnw -DloadTest=true -DloadTest.requests=100 -DloadTest.concurrency=10 test
```

Frontend type-check/lint and production build:

```bash
cd frontend
npm ci
npm run lint
npm run build
```

Database query indexes are managed by the Flyway migration in `src/main/resources/db/migration`.

## Docker and CI

The root [Dockerfile](Dockerfile) builds the backend JAR in a multi-stage image. [frontend/Dockerfile](frontend/Dockerfile) builds the React bundle and serves it with Nginx, including `/api` proxying, SSE support, and SPA fallback. `docker-compose.yml` is the development stack. Production uses [docker-compose.production.yml](docker-compose.production.yml) with Caddy HTTPS on ports 80/443; the backend, PostgreSQL, Kafka, Redis, and frontend remain private. See [docs/deployment.md](docs/deployment.md).

```bash
docker compose down
```

GitHub Actions in `.github/workflows/ci.yml` runs backend Maven verification and frontend npm lint/build on pushes and pull requests. It does not deploy or publish artifacts.
