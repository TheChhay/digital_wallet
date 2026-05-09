# Digital Wallet Backend Architecture

This backend follows a clean handler/service/repository structure:

- `cmd/api`: application entrypoint, graceful shutdown, dependency wiring.
- `internal/routes`: Gin route registration and route groups.
- `internal/handlers`: HTTP concerns only: bind, validate, response codes.
- `internal/services`: business rules: auth, KYC, transfer, idempotency, balances.
- `internal/repositories`: database access through GORM.
- `internal/models`: persistence models and enum-like constants.
- `internal/dto`: request/response structs.
- `internal/middleware`: JWT auth, admin guard, rate limit, logging, recovery.
- `migrations`: Goose PostgreSQL schema.

## Architecture Decisions

Business logic lives in services so handlers stay thin and repositories stay data-focused. Repositories expose methods such as `GetWalletForUpdate` rather than generic database access, which makes transaction safety visible in the service layer.

Money is stored as integer cents (`BIGINT`) to avoid floating-point rounding bugs. Transfers use a PostgreSQL transaction plus `SELECT ... FOR UPDATE` through GORM locking. Sender and receiver wallets are locked in UUID order to reduce deadlock risk.

Refresh tokens are random opaque values. Only SHA-256 hashes are stored. Refresh rotates tokens by revoking the old row and creating a new row in the same database transaction.

## Example Requests

Register:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "phone": "+85512345678",
  "password": "StrongPass123",
  "full_name": "Sophea Chan"
}
```

Transfer:

```http
POST /api/v1/wallet/transfer
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "receiver_phone": "+85587654321",
  "amount_cents": 2500,
  "description": "Dinner split",
  "idempotency_key": "client-generated-uuid-001"
}
```

Standard response:

```json
{
  "success": true,
  "message": "Transfer successful",
  "data": {
    "reference": "TRF-abc12345-177823..."
  }
}
```

## Running

1. Create a PostgreSQL database.
2. Copy `.env.example` to `.env` and update values.
3. Run migrations:

```bash
goose -dir migrations postgres "$DATABASE_URL" up
```

4. Start the API:

```bash
go run ./cmd/api
```

## Tests

```bash
go test ./...
```

The included unit test demonstrates the transfer invariant that a wallet cannot go negative.
