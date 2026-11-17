# rewards-points-api

A loyalty rewards REST API built with Java 21, Spring Boot 3.4, and PostgreSQL.
Members earn and redeem reward points; the balance can never go negative even under concurrent requests.

---

## Endpoints

| Method | Path | Body | Success | Error |
|--------|------|------|---------|-------|
| `POST` | `/members` | `{"name":"...", "email":"..."}` | 201 | 400 validation, 409 duplicate email |
| `POST` | `/members/{id}/earn` | `{"points": N}` | 200 `{"balance": N}` | 400, 404 |
| `POST` | `/members/{id}/redeem` | `{"points": N}` | 200 `{"balance": N}` | 409 insufficient balance |
| `GET` | `/members/{id}` | — | 200 member + history | 404 |

### Example curl commands

```bash
# 1. Create a member
curl -s -X POST http://localhost:8080/members \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}' | jq .

# 2. Earn 100 points
curl -s -X POST http://localhost:8080/members/1/earn \
  -H "Content-Type: application/json" \
  -d '{"points":100}' | jq .

# 3. Redeem 30 points
curl -s -X POST http://localhost:8080/members/1/redeem \
  -H "Content-Type: application/json" \
  -d '{"points":30}' | jq .

# 4. Get member (balance + history newest-first)
curl -s http://localhost:8080/members/1 | jq .

# 5. Try to overdraw (expect 409)
curl -s -X POST http://localhost:8080/members/1/redeem \
  -H "Content-Type: application/json" \
  -d '{"points":999}' | jq .
```

---

## How to run

### Prerequisites
- Java 21
- Maven 3.9+
- Docker + Docker Compose

### Start the database

```bash
cp .env.example .env
# Edit .env to set your preferred password
docker compose up -d
```

### Run the application

```bash
mvn spring-boot:run
```

### Run all tests (unit + integration + concurrency + JaCoCo)

```bash
mvn verify
```

---

## Design

The key design challenge is preventing two simultaneous redeem calls from both succeeding when only one has enough balance. This service solves it with a **pessimistic row-level lock** (`SELECT … FOR UPDATE` via Spring Data JPA's `@Lock(PESSIMISTIC_WRITE)`). When two redeem requests arrive at the same time, the database queues them: the first one commits or rolls back before the second one reads the balance. This guarantees the balance never goes negative without needing application-level synchronisation.

The trade-off is that concurrent redeems for the **same member** are serialised (queued at the DB), while different members are unaffected. This is acceptable for a portfolio-scale service.

---

## Measured results

<!-- Results filled from AI_Context/Results.md after mvn verify runs -->

| Metric | Value |
|--------|-------|
| `mvn verify` | see Results.md |
| JaCoCo line coverage | see Results.md |
| Concurrency test (×5 runs) | see Results.md |
