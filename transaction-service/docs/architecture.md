# PulseOps architecture

PulseOps is a React operations dashboard backed by a JWT-protected Spring Boot transaction API. PostgreSQL is the system of record, Kafka carries transaction-created events, and Redis handles idempotency and rate limiting.

## System architecture

```mermaid
flowchart LR
    operator[Operations user]
    frontend[React dashboard<br/>Vite + Tailwind]
    nginx[Nginx production container<br/>SPA fallback + /api proxy]
    api[Spring Boot transaction-service<br/>JWT security + REST + SSE]
    postgres[(PostgreSQL<br/>transactions + status history)]
    kafka[(Kafka<br/>transaction.created)]
    redis[(Redis<br/>idempotency + rate limits)]
    producer[JSON event producer]
    consumer[Kafka consumer]
    flow[Transaction processing<br/>RECEIVED → VALIDATING<br/>→ PROCESSING → COMPLETED]

    operator --> frontend
    frontend --> nginx
    nginx -->|REST /api/v1/transactions| api
    nginx -->|SSE status stream| api
    api -->|read/write transactions<br/>and status history| postgres
    api -->|reserve idempotency key<br/>and check rate limit| redis
    api --> producer
    producer -->|publish| kafka
    kafka --> consumer
    consumer --> flow
    flow -->|persist status changes| postgres
    flow -->|publish live updates| api
```

## Transaction creation and processing flow

```mermaid
sequenceDiagram
    participant UI as React dashboard
    participant API as Spring Boot API
    participant R as Redis
    participant DB as PostgreSQL
    participant K as Kafka
    participant C as Transaction consumer

    UI->>API: POST /api/v1/transactions<br/>JWT + Idempotency-Key
    API->>R: Check rate limit and reserve key
    R-->>API: Allowed / duplicate / exceeded
    API->>DB: Save transaction as RECEIVED
    API->>R: Store idempotency result
    API->>K: Publish transaction.created JSON event
    API-->>UI: Return transaction
    K-->>C: Deliver TransactionCreatedEvent
    C->>DB: Save VALIDATING
    C->>DB: Save PROCESSING
    C->>DB: Save COMPLETED
    Note over C: Processing failures are logged and rethrown;
    Note over C: no FAILED status transition is persisted
    UI->>API: GET /{id}/status/stream
    API-->>UI: SSE status history updates
```

## Component responsibilities

| Component | Responsibility |
|---|---|
| React dashboard | Authenticated operations UI, transaction creation, search/filter/pagination, charts, detail history, and live status display |
| Nginx | Serves the production frontend bundle, proxies `/api`, and provides SPA route fallback |
| Spring Boot API | JWT authentication, validation, transaction endpoints, status history, SSE, structured logging, and Actuator endpoints |
| PostgreSQL | Persists transactions and status history |
| Kafka | Delivers `transaction.created` JSON events asynchronously |
| Redis | Provides idempotency protection and creation rate limiting |

## Local Docker topology

Docker Compose starts the frontend on `localhost:3001`, the API on `localhost:8080`, PostgreSQL on `localhost:5434`, Kafka on `localhost:9092`, and Redis on `localhost:6379`. Inside the Compose network, the application uses the service names `postgres`, `kafka`, and `redis`.
