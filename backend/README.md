# Digital Wallet Backend

Backend API for a digital wallet application. It provides user authentication, wallet balance management, deposits, withdrawals, transfers, KYC submission, transaction history, and admin management endpoints.

## Features

- User registration and login
- JWT access tokens and refresh token rotation
- Wallet balance lookup
- Deposit and withdrawal endpoints
- Wallet-to-wallet transfers with idempotency support
- Transaction history with pagination and filters
- KYC submission and admin review
- Admin user and transaction management
- Request logging, recovery middleware, and rate limiting

## Tech Stack

- Go
- Gin
- PostgreSQL
- GORM
- JWT
- Goose migrations
- Zap logger

## Project Structure

```text
cmd/api               Application entrypoint and server startup
internal/config       Environment, database, and logger configuration
internal/routes       API route registration
internal/handlers     HTTP handlers for requests and responses
internal/services     Business logic
internal/repositories Database access through GORM
internal/models       Database models and constants
internal/dto          Request and response DTOs
internal/middleware   Auth, admin guard, logging, recovery, and rate limit middleware
internal/utils        Shared response and security helpers
migrations            PostgreSQL database migrations
docs                  Architecture notes and OpenAPI specification
```

## Requirements

- Go 1.26 or newer
- PostgreSQL
- Goose migration tool

## Environment Setup

Copy the example environment file:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Update the values in `.env` for your local database and secrets.

```env
APP_ENV=development
HTTP_PORT=8080
DATABASE_URL=postgres://wallet_user:wallet_password@localhost:5432/digital_wallet?sslmode=disable
JWT_SECRET=change-me-to-a-random-64-character-secret-for-production
ACCESS_TOKEN_TTL_MINUTES=15
REFRESH_TOKEN_TTL_HOURS=720
RATE_LIMIT_RPS=5
RATE_LIMIT_BURST=20
UPLOAD_DIR=upload
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET=digital_wallet
R2_ENDPOINT=
R2_PUBLIC_BASE_URL=
R2_REGION=auto
```

Do not commit your real `.env` file. It may contain database credentials and JWT secrets.

## Image Uploads

In development, uploaded images are stored locally in the `upload` folder and served from:

```text
/uploads
```

In production, uploaded images are stored in Cloudflare R2. Set these environment variables:

```env
R2_ACCOUNT_ID=your-account-id
R2_ACCESS_KEY_ID=your-access-key-id
R2_SECRET_ACCESS_KEY=your-secret-access-key
R2_BUCKET=digital_wallet
R2_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
R2_PUBLIC_BASE_URL=https://your-public-r2-domain
R2_REGION=auto
```

Supported image formats are JPEG, PNG, and WebP. The maximum image size is 5MB.

## Database Setup

Create a PostgreSQL database that matches your `DATABASE_URL`, then run migrations:

```bash
goose -dir migrations postgres "$DATABASE_URL" up
```

On Windows PowerShell, if `DATABASE_URL` is loaded in your environment:

```powershell
goose -dir migrations postgres $env:DATABASE_URL up
```

## Seed Test Users

After migrations, create or update one admin and two test users:

```bash
go run ./cmd/seed
```

Seed credentials:

```text
Admin  phone=+85510000001 password=Password123!
User A phone=+85510000002 password=Password123!
User B phone=+85510000003 password=Password123!
```

The seed command is idempotent, so it can be run multiple times. It resets the seed users' names, roles, statuses, passwords, and wallet balances.

## Run the API

Install dependencies:

```bash
go mod tidy
```

Start the API server:

```bash
go run ./cmd/api
```

By default, the API runs at:

```text
http://localhost:8080
```

Health check:

```text
GET /health
```

## API Endpoints

Base API path:

```text
/api/v1
```

Main endpoints:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/me
PUT  /api/v1/me/profile-image
POST /api/v1/me/kyc
GET  /api/v1/wallet
POST /api/v1/wallet/deposit
POST /api/v1/wallet/withdraw
POST /api/v1/wallet/transfer
GET  /api/v1/wallet/transactions
POST /api/v1/admin/login
GET  /api/v1/admin/users
GET  /api/v1/admin/transactions
PUT  /api/v1/admin/users/:userID/kyc
PUT  /api/v1/admin/users/:userID/status
```

List endpoints use cursor pagination. Send `limit` on the first request, then pass the returned `next_cursor` to get the next page:

```text
GET /api/v1/wallet/transactions?limit=20
GET /api/v1/wallet/transactions?limit=20&cursor=<next_cursor>
GET /api/v1/admin/users?limit=20&cursor=<next_cursor>
GET /api/v1/admin/transactions?limit=20&type=transfer&status=success&cursor=<next_cursor>
```

Cursor response shape:

```json
{
  "items": [],
  "limit": 20,
  "next_cursor": "eyJjcmVhdGVkX2F0Ijoi...",
  "has_more": true
}
```

Upload a profile image with multipart form data:

```http
PUT /api/v1/me/profile-image
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

image=<image file>
```

Submit KYC with multipart form data:

```http
POST /api/v1/me/kyc
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

full_name=Sophea Chan
dob=1998-04-20
address=Phnom Penh, Cambodia
id_card_image=<image file>
selfie_image=<image file>
```

For the full API contract, see:

```text
docs/openapi.yaml
```

## Example Requests

Register a user:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "phone": "+85512345678",
  "password": "StrongPass123",
  "full_name": "Sophea Chan"
}
```

Transfer money:

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

## Tests

Run all tests:

```bash
go test ./...
```

## Architecture

This backend uses a handler/service/repository structure:

- Handlers manage HTTP input and output.
- Services contain business rules.
- Repositories handle database access.
- Middleware handles cross-cutting concerns like authentication, logging, recovery, and rate limiting.

For more detail, see:

```text
docs/architecture.md
```

## Notes

- Money values are stored as integer cents to avoid floating-point rounding issues.
- Transfers use database transactions and row-level locking.
- Refresh tokens are stored as hashes and rotated when refreshed.
- Use a strong `JWT_SECRET` in production.
