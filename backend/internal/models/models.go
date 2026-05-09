package models

import (
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

type Role string
type UserStatus string
type KYCStatus string
type TransactionType string
type TransactionStatus string

const (
	RoleUser  Role = "user"
	RoleAdmin Role = "admin"

	UserActive UserStatus = "active"
	UserFrozen UserStatus = "frozen"

	KYCPending  KYCStatus = "pending"
	KYCApproved KYCStatus = "approved"
	KYCRejected KYCStatus = "rejected"

	TransactionDeposit  TransactionType = "deposit"
	TransactionWithdraw TransactionType = "withdraw"
	TransactionTransfer TransactionType = "transfer"

	TransactionPending TransactionStatus = "pending"
	TransactionSuccess TransactionStatus = "success"
	TransactionFailed  TransactionStatus = "failed"
)

type BaseModel struct {
	ID        uuid.UUID `gorm:"type:uuid;primaryKey;default:gen_random_uuid()" json:"id"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

func (b *BaseModel) BeforeCreate(_ *gorm.DB) error {
	if b.ID == uuid.Nil {
		b.ID = uuid.New()
	}
	return nil
}

type User struct {
	BaseModel
	Phone           string     `gorm:"type:varchar(32);uniqueIndex;not null" json:"phone"`
	PasswordHash    string     `gorm:"type:text;not null" json:"-"`
	FullName        string     `gorm:"type:varchar(160)" json:"full_name"`
	ProfileImageURL string     `gorm:"type:text" json:"profile_image_url"`
	Role            Role       `gorm:"type:varchar(20);not null;default:'user';index" json:"role"`
	Status          UserStatus `gorm:"type:varchar(20);not null;default:'active';index" json:"status"`
	Wallet          *Wallet    `json:"wallet,omitempty"`
}

type Wallet struct {
	BaseModel
	UserID       uuid.UUID `gorm:"type:uuid;not null;uniqueIndex" json:"user_id"`
	BalanceCents int64     `gorm:"not null;default:0;check:balance_cents >= 0" json:"balance_cents"`
	User         *User     `gorm:"constraint:OnDelete:CASCADE" json:"-"`
}

type Transaction struct {
	BaseModel
	Reference      string            `gorm:"type:varchar(64);uniqueIndex;not null" json:"reference"`
	IdempotencyKey string            `gorm:"type:varchar(120);index" json:"idempotency_key"`
	Type           TransactionType   `gorm:"type:varchar(20);not null;index" json:"type"`
	Status         TransactionStatus `gorm:"type:varchar(20);not null;index" json:"status"`
	AmountCents    int64             `gorm:"not null;check:amount_cents > 0" json:"amount_cents"`
	SenderID       *uuid.UUID        `gorm:"type:uuid;index" json:"sender_id"`
	ReceiverID     *uuid.UUID        `gorm:"type:uuid;index" json:"receiver_id"`
	Description    string            `gorm:"type:varchar(255)" json:"description"`
	FailureReason  string            `gorm:"type:varchar(255)" json:"failure_reason"`
	Sender         *User             `gorm:"foreignKey:SenderID" json:"-"`
	Receiver       *User             `gorm:"foreignKey:ReceiverID" json:"-"`
}

type KYCVerification struct {
	BaseModel
	UserID          uuid.UUID  `gorm:"type:uuid;not null;uniqueIndex" json:"user_id"`
	FullName        string     `gorm:"type:varchar(160);not null" json:"full_name"`
	DOB             time.Time  `gorm:"type:date;not null" json:"dob"`
	Address         string     `gorm:"type:text;not null" json:"address"`
	IDCardImageURL  string     `gorm:"type:text;not null" json:"id_card_image_url"`
	SelfieImageURL  string     `gorm:"type:text;not null" json:"selfie_image_url"`
	Status          KYCStatus  `gorm:"type:varchar(20);not null;default:'pending';index" json:"status"`
	RejectionReason string     `gorm:"type:varchar(255)" json:"rejection_reason"`
	ReviewedBy      *uuid.UUID `gorm:"type:uuid" json:"reviewed_by"`
	ReviewedAt      *time.Time `json:"reviewed_at"`
	User            *User      `gorm:"constraint:OnDelete:CASCADE" json:"-"`
}

type RefreshToken struct {
	BaseModel
	UserID              uuid.UUID  `gorm:"type:uuid;not null;index" json:"user_id"`
	TokenHash           string     `gorm:"type:text;not null;uniqueIndex" json:"-"`
	ExpiresAt           time.Time  `gorm:"not null;index" json:"expires_at"`
	RevokedAt           *time.Time `json:"revoked_at"`
	ReplacedByTokenHash string     `gorm:"type:text" json:"-"`
	User                *User      `gorm:"constraint:OnDelete:CASCADE" json:"-"`
}

type AuditLog struct {
	BaseModel
	ActorID   *uuid.UUID `gorm:"type:uuid;index" json:"actor_id"`
	Action    string     `gorm:"type:varchar(80);not null;index" json:"action"`
	Entity    string     `gorm:"type:varchar(80);not null;index" json:"entity"`
	EntityID  *uuid.UUID `gorm:"type:uuid;index" json:"entity_id"`
	IPAddress string     `gorm:"type:varchar(80)" json:"ip_address"`
	UserAgent string     `gorm:"type:text" json:"user_agent"`
	Metadata  string     `gorm:"type:jsonb;default:'{}'" json:"metadata"`
}
