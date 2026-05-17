package dto

import (
	"time"

	"github.com/google/uuid"
)

// ─────────────────────────────────────────────
// Generic
// ─────────────────────────────────────────────

type APIResponse struct {
	Success bool        `json:"success"`
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
	Errors  interface{} `json:"errors,omitempty"`
}

// FIX #5: generic CursorResponse for type safety
type CursorResponse[T any] struct {
	Items      []T    `json:"items"`
	Limit      int    `json:"limit"`
	NextCursor string `json:"next_cursor,omitempty"`
	HasMore    bool   `json:"has_more"`
}

// ─────────────────────────────────────────────
// Auth
// ─────────────────────────────────────────────

type RegisterRequest struct {
	Phone     string `json:"phone"      binding:"required,min=7,max=32"`
	Password  string `json:"password"   binding:"required,min=8,max=72"`
	FirstName string `json:"first_name" binding:"required,min=1,max=80"`
	LastName  string `json:"last_name"  binding:"required,min=1,max=80"`
}

type LoginRequest struct {
	Phone    string `json:"phone"    binding:"required"`
	Password string `json:"password" binding:"required"`
}

type RefreshRequest struct {
	RefreshToken string `json:"refresh_token" binding:"required"`
}

type AuthResponse struct {
	AccessToken  string       `json:"access_token"`
	RefreshToken string       `json:"refresh_token"`
	User         UserResponse `json:"user"`
}

// ─────────────────────────────────────────────
// User / Profile
// ─────────────────────────────────────────────

type UserResponse struct {
	ID              uuid.UUID `json:"id"`
	Phone           string    `json:"phone"`
	FirstName       string    `json:"first_name"`
	LastName        string    `json:"last_name"`
	Role            string    `json:"role"`
	Status          string    `json:"status"`
	ProfileImageURL string    `json:"profile_image_url"`
	CreatedAt       time.Time `json:"created_at"`
}

type UserInfoResponse struct {
	ID       uuid.UUID `json:"id"`
	FullName string    `json:"full_name"`
	Phone    string    `json:"phone"`
}

type AccountStatusRequest struct {
	Status string `json:"status" binding:"required,oneof=active frozen"`
}

type KYCSubmitRequest struct {
	FullName       string `json:"full_name"         binding:"required,min=2,max=160"`
	DOB            string `json:"dob"               binding:"required"`
	Address        string `json:"address"           binding:"required,min=10"`
	IDCardImageURL string `json:"id_card_image_url" binding:"required,url"`
	SelfieImageURL string `json:"selfie_image_url"  binding:"required,url"`
}

type KYCReviewRequest struct {
	Status          string `json:"status"           binding:"required,oneof=approved rejected"`
	RejectionReason string `json:"rejection_reason"`
}

// ─────────────────────────────────────────────
// Wallet / Money
// ─────────────────────────────────────────────

type MoneyRequest struct {
	AmountCents int64  `json:"amount_cents" binding:"required,gt=0"`
	Description string `json:"description"  binding:"omitempty,max=255"`
}

type TransferRequest struct {
	ReceiverPhone  string `json:"receiver_phone"  binding:"required"`
	AmountCents    int64  `json:"amount_cents"    binding:"required,gt=0"`
	Description    string `json:"description"     binding:"omitempty,max=255"`
	IdempotencyKey string `json:"idempotency_key" binding:"required,min=12,max=120"`
}

// ─────────────────────────────────────────────
// Transactions
// ─────────────────────────────────────────────

type TransactionFilter struct {
	Cursor string `form:"cursor"`
	Limit  int    `form:"limit"`
	// FIX #4: restrict accepted values to prevent garbage DB queries
	Type   string `form:"type"   binding:"omitempty,oneof=deposit withdraw transfer"`
	Status string `form:"status" binding:"omitempty,oneof=success failed pending"`

	// Populated internally after cursor decode — not from the request
	CursorCreatedAt time.Time
	CursorID        uuid.UUID
}

type TransactionResponse struct {
	ID            uuid.UUID  `json:"id"`
	Reference     string     `json:"reference"`
	Type          string     `json:"type"`
	Status        string     `json:"status"`
	AmountCents   int64      `json:"amount_cents"`
	Description   string     `json:"description"`
	CreatedAt     time.Time  `json:"created_at"`
	SenderID      *uuid.UUID `json:"sender_id,omitempty"`
	ReceiverID    *uuid.UUID `json:"receiver_id,omitempty"`
	MerchantName  string     `json:"merchant_name,omitempty"`
	ReceiverName  string     `json:"receiver_name,omitempty"`
	ReceiverPhone string     `json:"receiver_phone,omitempty"`
	IsPositive    *bool      `json:"is_positive,omitempty"`
}

// ─────────────────────────────────────────────
// QR — Dynamic
// ─────────────────────────────────────────────

type GenerateQRRequest struct {
	// FIX #3: uuid binding tag validates format before hitting service layer
	WalletID string  `json:"wallet_id" binding:"required,uuid"`
	Amount   float64 `json:"amount"    binding:"required,gt=0"`
	Currency string  `json:"currency"  binding:"required"`
}

// FIX #1: renamed field to match services.go (QRImageBase64 not QRImageUrl)
type GenerateQRResponse struct {
	QRImageBase64 string `json:"qr_image_base64"`
	Token         string `json:"token"`
	ExpiresAt     int64  `json:"expires_at"`
}

type ValidateTokenRequest struct {
	Token string `json:"token" binding:"required"`
}

// FIX #6: Message always populated for consistent client handling
type ValidateTokenResponse struct {
	IsValid        bool    `json:"is_valid"`
	Message        string  `json:"message"`
	RecipientName  string  `json:"recipient_name,omitempty"`
	RecipientPhone string  `json:"recipient_phone,omitempty"`
	Amount         float64 `json:"amount,omitempty"`
	Currency       string  `json:"currency,omitempty"`
}

// ─────────────────────────────────────────────
// QR — Static (FIX #2: new struct)
// ─────────────────────────────────────────────

type StaticQRRequest struct {
	WalletID string `json:"wallet_id" binding:"required,uuid"`
}

type StaticQRResponse struct {
	QRImageBase64 string `json:"qr_image_base64"`
	WalletID      string `json:"wallet_id"`
}
