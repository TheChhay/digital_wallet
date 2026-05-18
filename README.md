# Digital Wallet

Full-stack digital wallet project with an Android client and a Go backend API. The app supports account registration and login, JWT sessions, wallet balance lookup, deposits, withdrawals, transfers, transaction history, KYC submission, and QR payment flows.

## Repository Layout

```text
.
├── DigitalWallet/          Android app built with Kotlin and Jetpack Compose
└── backend/                Go API, PostgreSQL migrations, OpenAPI docs, seed command
```

## Stack

**Android app**

- Kotlin
- Jetpack Compose and Material 3
- Hilt dependency injection
- Retrofit, OkHttp, and kotlinx.serialization
- CameraX and ML Kit barcode scanning
- AndroidX Security Crypto for local token storage

**Backend**

- Go
- Gin HTTP router
- PostgreSQL
- GORM
- JWT access tokens and refresh-token rotation
- Goose migrations
- Zap logging

## How The Pieces Fit Together

The Android app talks to the backend through Retrofit services in:

```text
DigitalWallet/app/src/main/java/com/app/digitalwallet/api
```

The Retrofit base URL is configured in:

```text
DigitalWallet/app/src/main/java/com/app/digitalwallet/di/NetworkModule.kt
```

For the Android emulator, the app uses:

```text
http://10.0.2.2:8080/api/v1/
```

`10.0.2.2` is the emulator alias for the host machine, so it reaches the Go API running locally on port `8080`. If you run the app on a physical phone, change `BASE_HOST` to your computer's LAN IP address, for example:

```kotlin
const val BASE_HOST = "http://192.168.1.25:8080"
```

The backend exposes API routes from:

```text
backend/internal/routes/routes.go
```

The main API prefix is:

```text
/api/v1
```

## Main Features

- Register and log in with phone number and password
- Store access and refresh tokens on the Android client
- Automatically refresh expired access tokens
- View wallet balance and transaction history
- Deposit, withdraw, and transfer money
- Look up transfer recipients
- Submit KYC identity data and images
- Generate, validate, and scan QR payment data
- Admin login, user review, KYC review, and account status management

## Backend Architecture

The backend uses a handler/service/repository structure:

```text
backend/cmd/api             API entrypoint and server startup
backend/cmd/seed            Local seed users command
backend/internal/routes     Gin route registration
backend/internal/handlers   HTTP request binding and responses
backend/internal/services   Business rules and transactions
backend/internal/repositories Database access through GORM
backend/internal/models     Database models
backend/internal/dto        Request and response DTOs
backend/internal/middleware Auth, admin guard, logging, recovery, rate limit
backend/internal/storage    Local or R2 image storage
backend/migrations          PostgreSQL schema migrations
backend/docs                OpenAPI and architecture docs
```

Money is stored as integer cents in the backend to avoid floating-point rounding problems. Transfers use database transactions and wallet row locking so balances remain consistent.

## Android Architecture

The Android app follows a common Compose MVVM shape:

```text
DigitalWallet/app/src/main/java/com/app/digitalwallet/api        Retrofit APIs
DigitalWallet/app/src/main/java/com/app/digitalwallet/api/dto    Network DTOs
DigitalWallet/app/src/main/java/com/app/digitalwallet/auth       Token/session helpers
DigitalWallet/app/src/main/java/com/app/digitalwallet/data       Repositories
DigitalWallet/app/src/main/java/com/app/digitalwallet/di         Hilt modules
DigitalWallet/app/src/main/java/com/app/digitalwallet/navigation Navigation routes
DigitalWallet/app/src/main/java/com/app/digitalwallet/ui         Compose screens/components/theme
DigitalWallet/app/src/main/java/com/app/digitalwallet/viewmodel  Screen state and actions
```

The app entrypoints are:

```text
DigitalWallet/app/src/main/java/com/app/digitalwallet/DigitalWalletApp.kt
DigitalWallet/app/src/main/java/com/app/digitalwallet/MainActivity.kt
```

## Prerequisites

- Go 1.26.1 or newer
- PostgreSQL
- Goose migration tool
- Android Studio
- Android SDK with compile SDK 36 support
- JDK 11 or newer

## Backend Setup

From the backend folder:

```powershell
cd backend
Copy-Item .env.example .env
```

Edit `backend/.env` for your local database and secret values. The important variables are:

```env
APP_ENV=development
HTTP_PORT=8080
DATABASE_URL=postgres://wallet_user:wallet_password@localhost:5432/digital_wallet?sslmode=disable
JWT_SECRET=change-me-to-a-random-64-character-secret
ACCESS_TOKEN_TTL_MINUTES=15
REFRESH_TOKEN_TTL_HOURS=720
RATE_LIMIT_RPS=5
RATE_LIMIT_BURST=20
UPLOAD_DIR=upload
```

Create the PostgreSQL database named in `DATABASE_URL`, then run migrations:

```powershell
goose -dir migrations postgres $env:DATABASE_URL up
```

Install or sync Go dependencies:

```powershell
go mod tidy
```

Seed local users:

```powershell
go run ./cmd/seed
```

Seed credentials:

```text
Admin  phone=+85510000001 password=Password123!
User A phone=+85510000002 password=Password123!
User B phone=+85510000003 password=Password123!
```

Start the API:

```powershell
go run ./cmd/api
```

The backend should be available at:

```text
http://localhost:8080
```

Useful backend URLs:

```text
GET /health
GET /swagger
GET /openapi.yaml
```

## Android Setup

Open the `DigitalWallet` folder in Android Studio.

Before running the app, make sure the backend is running on port `8080`. For an Android emulator, the current `BASE_HOST` setting should work:

```kotlin
const val BASE_HOST = "http://10.0.2.2:8080"
```

Run the app from Android Studio, or from the command line:

```powershell
cd DigitalWallet
.\gradlew.bat assembleDebug
```

Run Android unit tests:

```powershell
.\gradlew.bat test
```

## API Areas

Backend routes are grouped by purpose:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout

GET  /api/v1/me
PUT  /api/v1/me/profile
PUT  /api/v1/me/profile-image
GET  /api/v1/me/kyc
POST /api/v1/me/kyc

GET  /api/v1/wallet
GET  /api/v1/wallet/get-userinfo-by-wallet-id
POST /api/v1/wallet/deposit
POST /api/v1/wallet/withdraw
POST /api/v1/wallet/transfer
GET  /api/v1/wallet/transactions

POST /api/v1/qr/generate
POST /api/v1/qr/validate
GET  /api/v1/qr/static

POST /api/v1/admin/login
GET  /api/v1/admin/users
GET  /api/v1/admin/transactions
PUT  /api/v1/admin/users/:userID/kyc
PUT  /api/v1/admin/users/:userID/status
```

For the full API contract, see:

```text
backend/docs/openapi.yaml
```

## Common Development Flow

1. Start PostgreSQL.
2. Run backend migrations.
3. Start the backend with `go run ./cmd/api`.
4. Open `DigitalWallet` in Android Studio.
5. Run the Android app on an emulator.
6. Use seed users or register a new user.

## Testing

Backend tests:

```powershell
cd backend
go test ./...
```

Android tests:

```powershell
cd DigitalWallet
.\gradlew.bat test
```

## Troubleshooting

If the Android emulator cannot reach the backend, confirm the backend is running at `http://localhost:8080`, then keep `BASE_HOST` as `http://10.0.2.2:8080`.

If a physical Android device cannot reach the backend, replace `10.0.2.2` with your computer's LAN IP address and make sure the phone and computer are on the same network.

If the backend fails on startup, check that `DATABASE_URL` is set, PostgreSQL is running, migrations have been applied, and `JWT_SECRET` is at least 32 characters long.

If image upload URLs fail locally, remember that development uploads are stored under `backend/upload` and served from `/uploads` only when `APP_ENV` is not `production`.

## More Documentation

- Backend README: `backend/README.md`
- Backend architecture notes: `backend/docs/architecture.md`
- OpenAPI spec: `backend/docs/openapi.yaml`
