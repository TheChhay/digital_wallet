package routes_test

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"digital_wallet_api/internal/config"
	"digital_wallet_api/internal/handlers"
	"digital_wallet_api/internal/models"
	"digital_wallet_api/internal/repositories"
	"digital_wallet_api/internal/routes"
	"digital_wallet_api/internal/services"
	"digital_wallet_api/internal/storage"
	"digital_wallet_api/internal/utils"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var testDB *gorm.DB

const testJWTSecret = "test-secret-must-be-at-least-32-characters"

func TestTransferSuccessUpdatesBalances(t *testing.T) {
	router, cfg := setupTransferTest(t)
	sender := createTestUser(t, models.RoleUser, "+85590000001", 10000)
	receiver := createTestUser(t, models.RoleUser, "+85590000002", 2500)
	token := accessToken(t, cfg, sender.ID, models.RoleUser)

	body := map[string]interface{}{
		"receiver_phone":  receiver.Phone,
		"amount_cents":    3500,
		"description":     "integration transfer",
		"idempotency_key": "transfer-success-001",
	}
	resp := performJSONRequest(router, http.MethodPost, "/api/v1/wallet/transfer", body, token)

	assert.Equal(t, http.StatusCreated, resp.Code)
	var payload map[string]interface{}
	assert.NoError(t, json.Unmarshal(resp.Body.Bytes(), &payload))
	assert.Equal(t, true, payload["success"])
	assert.Equal(t, "Transfer successful", payload["message"])
	assert.NotNil(t, payload["data"])

	assert.Equal(t, int64(6500), walletBalance(t, sender.ID))
	assert.Equal(t, int64(6000), walletBalance(t, receiver.ID))
}

func TestTransferFailsWithInsufficientBalance(t *testing.T) {
	router, cfg := setupTransferTest(t)
	sender := createTestUser(t, models.RoleUser, "+85590000003", 1000)
	receiver := createTestUser(t, models.RoleUser, "+85590000004", 2500)
	token := accessToken(t, cfg, sender.ID, models.RoleUser)

	body := map[string]interface{}{
		"receiver_phone":  receiver.Phone,
		"amount_cents":    2000,
		"description":     "too much",
		"idempotency_key": "transfer-fail-balance-001",
	}
	resp := performJSONRequest(router, http.MethodPost, "/api/v1/wallet/transfer", body, token)

	assert.Equal(t, http.StatusUnprocessableEntity, resp.Code)
	var payload map[string]interface{}
	assert.NoError(t, json.Unmarshal(resp.Body.Bytes(), &payload))
	assert.Equal(t, false, payload["success"])
	assert.Equal(t, "insufficient funds", payload["message"])

	assert.Equal(t, int64(1000), walletBalance(t, sender.ID))
	assert.Equal(t, int64(2500), walletBalance(t, receiver.ID))
}

func TestTransferFailsWithoutAuthorization(t *testing.T) {
	router, _ := setupTransferTest(t)
	receiver := createTestUser(t, models.RoleUser, "+85590000005", 2500)

	body := map[string]interface{}{
		"receiver_phone":  receiver.Phone,
		"amount_cents":    500,
		"idempotency_key": "transfer-fail-auth-001",
	}
	resp := performJSONRequest(router, http.MethodPost, "/api/v1/wallet/transfer", body, "")

	assert.Equal(t, http.StatusUnauthorized, resp.Code)
	var payload map[string]interface{}
	assert.NoError(t, json.Unmarshal(resp.Body.Bytes(), &payload))
	assert.Equal(t, false, payload["success"])
	assert.Equal(t, "Missing bearer token", payload["message"])
}

func TestReviewKYCFailsWithInvalidUUIDFormat(t *testing.T) {
	router, cfg := setupTransferTest(t)
	admin := createTestUser(t, models.RoleAdmin, "+85590000006", 0)
	token := accessToken(t, cfg, admin.ID, models.RoleAdmin)

	body := map[string]interface{}{
		"status": "approved",
	}
	resp := performJSONRequest(router, http.MethodPut, "/api/v1/admin/users/not-a-uuid/kyc", body, token)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	var payload map[string]interface{}
	assert.NoError(t, json.Unmarshal(resp.Body.Bytes(), &payload))
	assert.Equal(t, false, payload["success"])
	assert.Equal(t, "Invalid user id", payload["message"])
}

func setupTransferTest(t *testing.T) (*gin.Engine, *config.Config) {
	t.Helper()
	if os.Getenv("TEST_DATABASE_URL") == "" {
		t.Skip("set TEST_DATABASE_URL to run database integration tests")
	}
	gin.SetMode(gin.TestMode)
	testDB = setupTestDB(t)
	setupTestSchema(t)
	teardownTestDB(t)
	t.Cleanup(func() {
		teardownTestDB(t)
		sqlDB, err := testDB.DB()
		if err == nil {
			_ = sqlDB.Close()
		}
	})

	cfg := &config.Config{
		AppEnv:          "test",
		JWTSecret:       testJWTSecret,
		AccessTokenTTL:  15 * time.Minute,
		RefreshTokenTTL: 24 * time.Hour,
		RateLimitRPS:    1000,
		RateLimitBurst:  1000,
		UploadDir:       "upload-test",
	}
	repo := repositories.New(testDB)
	service := services.New(repo, cfg)
	handler := handlers.New(service, storage.New(cfg))
	return routes.Setup(cfg, zap.NewNop(), handler), cfg
}

func setupTestDB(t *testing.T) *gorm.DB {
	t.Helper()
	db, err := gorm.Open(postgres.Open(os.Getenv("TEST_DATABASE_URL")), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Silent),
	})
	if !assert.NoError(t, err) {
		t.FailNow()
	}
	return db
}

func setupTestSchema(t *testing.T) {
	t.Helper()
	if !assert.NoError(t, testDB.Exec("CREATE EXTENSION IF NOT EXISTS pgcrypto").Error) {
		t.FailNow()
	}
	err := testDB.AutoMigrate(
		&models.User{},
		&models.Wallet{},
		&models.Transaction{},
		&models.KYCVerification{},
		&models.RefreshToken{},
		&models.AuditLog{},
	)
	if !assert.NoError(t, err) {
		t.FailNow()
	}
}

func teardownTestDB(t *testing.T) {
	t.Helper()
	if !assert.NoError(t, testDB.Exec("TRUNCATE TABLE transactions, wallets, users RESTART IDENTITY CASCADE").Error) {
		t.FailNow()
	}
}

func createTestUser(t *testing.T, role models.Role, phone string, balanceCents int64) models.User {
	t.Helper()
	passwordHash, err := utils.HashPassword("Password123!")
	assert.NoError(t, err)

	user := models.User{
		BaseModel:    models.BaseModel{ID: uuid.New()},
		Phone:        phone,
		PasswordHash: passwordHash,
		FullName:     "Test " + string(role),
		Role:         role,
		Status:       models.UserActive,
	}
	if !assert.NoError(t, testDB.Create(&user).Error) {
		t.FailNow()
	}
	if !assert.NoError(t, testDB.Create(&models.Wallet{UserID: user.ID, BalanceCents: balanceCents}).Error) {
		t.FailNow()
	}
	return user
}

func accessToken(t *testing.T, cfg *config.Config, userID uuid.UUID, role models.Role) string {
	t.Helper()
	token, err := utils.GenerateAccessToken(cfg.JWTSecret, userID, string(role), cfg.AccessTokenTTL)
	assert.NoError(t, err)
	return token
}

func performJSONRequest(router *gin.Engine, method, path string, body interface{}, token string) *httptest.ResponseRecorder {
	raw, _ := json.Marshal(body)
	req := httptest.NewRequest(method, path, bytes.NewReader(raw))
	req.Header.Set("Content-Type", "application/json")
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	resp := httptest.NewRecorder()
	router.ServeHTTP(resp, req)
	return resp
}

func walletBalance(t *testing.T, userID uuid.UUID) int64 {
	t.Helper()
	var wallet models.Wallet
	assert.NoError(t, testDB.First(&wallet, "user_id = ?", userID).Error)
	return wallet.BalanceCents
}
