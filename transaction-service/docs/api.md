# PulseOps API

Base URL: `http://localhost:8080`

The transaction API is rooted at `/api/v1/transactions`. Every transaction endpoint requires a JWT bearer token. The application validates JWTs with the configured HMAC secret. There is no token-issuing endpoint in this service.

## Authentication

Send the token on protected requests:

```http
Authorization: Bearer <JWT>
```

`GET /actuator/health` is public. Requests to `/api/v1/**` without valid authentication are rejected by Spring Security.

## Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/transactions` | JWT required | Create a transaction |
| `GET` | `/api/v1/transactions` | JWT required | List, paginate, and filter transactions |
| `GET` | `/api/v1/transactions/{id}` | JWT required | Get one transaction |
| `GET` | `/api/v1/transactions/{id}/status` | JWT required | Get status history |
| `GET` | `/api/v1/transactions/{id}/status/stream` | JWT required | Stream status changes with SSE |
| `GET` | `/actuator/health` | Public | Check service health |

## Create a transaction

### Request

```http
POST /api/v1/transactions HTTP/1.1
Authorization: Bearer <JWT>
Content-Type: application/json
Idempotency-Key: order-123

{
  "amount": 42.50,
  "currency": "USD",
  "description": "Example transaction"
}
```

`Idempotency-Key` is required. `amount` is required and must be positive. `currency` is required, non-blank, and exactly three characters. `description` is optional.

### Success: `201 Created`

```json
{
  "id": "5cfdab03-6625-4c81-bd0c-3da6b3171814",
  "amount": 42.50,
  "currency": "USD",
  "description": "Example transaction",
  "status": "RECEIVED",
  "createdAt": "2026-10-03T06:34:00.617086388Z",
  "updatedAt": "2026-10-03T06:34:00.617086388Z"
}
```

The service stores the transaction and initial status history, then publishes a `TransactionCreatedEvent` to Kafka. Reusing an idempotency key returns the previously created transaction instead of creating a duplicate.

## List transactions

### Request

```http
GET /api/v1/transactions?page=0&size=20&status=COMPLETED HTTP/1.1
Authorization: Bearer <JWT>
```

Query parameters:

| Parameter | Default | Description |
|---|---:|---|
| `page` | `0` | Zero-based page number |
| `size` | `20` | Page size |
| `status` | omitted | Optional `TransactionStatus` filter |

Supported statuses are `RECEIVED`, `VALIDATING`, `PROCESSING`, `COMPLETED`, and `FAILED`.

### Success: `200 OK`

The endpoint returns Spring Data’s paged response containing transaction records and pagination metadata. A representative response is:

```json
{
  "content": [
    {
      "id": "5cfdab03-6625-4c81-bd0c-3da6b3171814",
      "amount": 42.50,
      "currency": "USD",
      "description": "Example transaction",
      "status": "COMPLETED",
      "createdAt": "2026-10-03T06:34:00.617086Z",
      "updatedAt": "2026-10-03T06:34:01.022493Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## Get a transaction

### Request

```http
GET /api/v1/transactions/5cfdab03-6625-4c81-bd0c-3da6b3171814 HTTP/1.1
Authorization: Bearer <JWT>
```

### Success: `200 OK`

```json
{
  "id": "5cfdab03-6625-4c81-bd0c-3da6b3171814",
  "amount": 42.50,
  "currency": "USD",
  "description": "Example transaction",
  "status": "COMPLETED",
  "createdAt": "2026-10-03T06:34:00.617086Z",
  "updatedAt": "2026-10-03T06:34:01.022493Z"
}
```

## Get status history

### Request

```http
GET /api/v1/transactions/5cfdab03-6625-4c81-bd0c-3da6b3171814/status HTTP/1.1
Authorization: Bearer <JWT>
```

### Success: `200 OK`

```json
[
  {
    "id": "b1d50bc0-f490-4ff1-bd63-59679009d716",
    "transactionId": "5cfdab03-6625-4c81-bd0c-3da6b3171814",
    "status": "RECEIVED",
    "changedAt": "2026-10-03T06:34:00.696422Z"
  },
  {
    "id": "bb8c0f42-8135-4a68-91ed-b942d561caa9",
    "transactionId": "5cfdab03-6625-4c81-bd0c-3da6b3171814",
    "status": "COMPLETED",
    "changedAt": "2026-10-03T06:34:01.023740Z"
  }
]
```

Entries are returned in ascending `changedAt` order.

## Stream status changes

### Request

```http
GET /api/v1/transactions/5cfdab03-6625-4c81-bd0c-3da6b3171814/status/stream HTTP/1.1
Authorization: Bearer <JWT>
Accept: text/event-stream
```

### Success: `200 OK`

The endpoint keeps an SSE connection open and emits status events for the transaction:

```text
event: status
data: {"id":"3289ef41-e5fc-4a75-928c-92a8be89cc4a","transactionId":"5cfdab03-6625-4c81-bd0c-3da6b3171814","status":"VALIDATING","changedAt":"2026-10-03T06:34:01.015506Z"}
```

The transaction is checked before the stream is created, so an unknown transaction produces the not-found response instead of opening a stream.

## Health

### Request

```http
GET /actuator/health HTTP/1.1
```

### Success: `200 OK`

```json
{
  "groups": ["liveness", "readiness"],
  "status": "UP"
}
```

## Error responses

The application’s controller exception handler returns these documented API errors:

### Validation: `400 Bad Request`

```json
{
  "error": "Validation failed",
  "fields": {
    "amount": "must be greater than 0",
    "currency": "size must be between 3 and 3"
  }
}
```

### Transaction not found: `404 Not Found`

```json
{
  "error": "Transaction not found"
}
```

### Rate limit exceeded: `429 Too Many Requests`

```json
{
  "error": "Rate limit exceeded"
}
```

The default transaction-creation limit is 10 requests per 60 seconds, configurable with `RATE_LIMIT_TRANSACTIONS` and `RATE_LIMIT_WINDOW_SECONDS`.
