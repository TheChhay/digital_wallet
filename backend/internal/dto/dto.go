package dto

import (
	"time"

	"github.com/google/uuid"
)

type APIResponse struct {
	Success bool        `json:"success"`
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
	Errors  interface{} `json:"errors,omitempty"`
}

type RegisterRequest struct {
	Phone     string `json:"phone" binding:"required,min=7,max=32"`
	Password  string `json:"password" binding:"required,min=8,max=72"`
	FirstName string `json:"first_name" binding:"required,min=1,max=80"`
	LastName  string `json:"last_name" binding:"required,min=1,max=80"`
}

type LoginRequest struct {
	Phone    string `json:"phone" binding:"required"`
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

type ProfileImageRequest struct {
	ProfileImageURL string `json:"profile_image_url" binding:"required,url"`
}

type KYCSubmitRequest struct {
	FullName       string `json:"full_name" binding:"required,min=2,max=160"`
	DOB            string `json:"dob" binding:"required"`
	Address        string `json:"address" binding:"required,min=10"`
	IDCardImageURL string `json:"id_card_image_url" binding:"required,url"`
	SelfieImageURL string `json:"selfie_image_url" binding:"required,url"`
}

type KYCReviewRequest struct {
	Status          string `json:"status" binding:"required,oneof=approved rejected"`
	RejectionReason string `json:"rejection_reason"`
}

type MoneyRequest struct {
	AmountCents int64  `json:"amount_cents" binding:"required,gt=0"`
	Description string `json:"description" binding:"omitempty,max=255"`
}

type TransferRequest struct {
	ReceiverPhone  string `json:"receiver_phone" binding:"required"`
	AmountCents    int64  `json:"amount_cents" binding:"required,gt=0"`
	Description    string `json:"description" binding:"omitempty,max=255"`
	IdempotencyKey string `json:"idempotency_key" binding:"required,min=12,max=120"`
}

type TransactionFilter struct {
	Cursor          string `form:"cursor"`
	Limit           int    `form:"limit"`
	Type            string `form:"type"`
	Status          string `form:"status"`
	CursorCreatedAt time.Time
	CursorID        uuid.UUID
}

type CursorResponse struct {
	Items      interface{} `json:"items"`
	Limit      int         `json:"limit"`
	NextCursor string      `json:"next_cursor,omitempty"`
	HasMore    bool        `json:"has_more"`
}

type AccountStatusRequest struct {
	Status string `json:"status" binding:"required,oneof=active frozen"`
}
