package repositories

import (
	"errors"

	"digital_wallet_api/internal/dto"
	"digital_wallet_api/internal/models"

	"github.com/google/uuid"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

var ErrNotFound = gorm.ErrRecordNotFound

type Repository struct {
	db *gorm.DB
}

func New(db *gorm.DB) *Repository {
	return &Repository{db: db}
}

func (r *Repository) WithTx(tx *gorm.DB) *Repository {
	return &Repository{db: tx}
}

func (r *Repository) DB() *gorm.DB {
	return r.db
}

func (r *Repository) CreateUser(user *models.User) error {
	return r.db.Create(user).Error
}

func (r *Repository) FindUserByID(id uuid.UUID) (*models.User, error) {
	var user models.User
	err := r.db.First(&user, "id = ?", id).Error
	return &user, err
}

func (r *Repository) FindUserByPhone(phone string) (*models.User, error) {
	var user models.User
	err := r.db.First(&user, "phone = ?", phone).Error
	return &user, err
}

func (r *Repository) UpdateUser(user *models.User) error {
	return r.db.Save(user).Error
}

func (r *Repository) ListUsers(f dto.TransactionFilter) ([]models.User, error) {
	var users []models.User
	q := r.db.Model(&models.User{})
	q = applyCursor(q, f)
	err := q.Order("created_at desc, id desc").Limit(f.Limit + 1).Find(&users).Error
	return users, err
}

func applyCursor(q *gorm.DB, f dto.TransactionFilter) *gorm.DB {
	if f.Cursor == "" {
		return q
	}
	return q.Where("(created_at < ? OR (created_at = ? AND id < ?))", f.CursorCreatedAt, f.CursorCreatedAt, f.CursorID)
}

func (r *Repository) CreateWallet(wallet *models.Wallet) error {
	return r.db.Create(wallet).Error
}

func (r *Repository) GetWalletByWalletID(walletID uuid.UUID) (*models.Wallet, error) {
	var wallet models.Wallet
	err := r.db.First(&wallet, "id = ?", walletID).Error
	return &wallet, err
}

func (r *Repository) GetWalletWithUserByWalletID(walletID uuid.UUID) (*models.Wallet, error) {
	var wallet models.Wallet
	err := r.db.Preload("User").First(&wallet, "id = ?", walletID).Error
	return &wallet, err
}

func (r *Repository) GetWalletForUpdate(userID uuid.UUID) (*models.Wallet, error) {
	var wallet models.Wallet
	err := r.db.Clauses(clause.Locking{Strength: "UPDATE"}).First(&wallet, "user_id = ?", userID).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, ErrNotFound
	}
	return &wallet, err
}

func (r *Repository) UpdateWallet(wallet *models.Wallet) error {
	return r.db.Save(wallet).Error
}

func (r *Repository) CreateTransaction(txn *models.Transaction) error {
	return r.db.Create(txn).Error
}

func (r *Repository) FindTransactionByIdempotencyKey(senderID uuid.UUID, key string) (*models.Transaction, error) {
	var txn models.Transaction
	err := r.db.Where("sender_id = ? AND idempotency_key = ?", senderID, key).First(&txn).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, ErrNotFound
	}
	return &txn, err
}

func (r *Repository) ListUserTransactions(userID uuid.UUID, f dto.TransactionFilter) ([]models.Transaction, error) {
	var txns []models.Transaction
	q := r.db.Model(&models.Transaction{}).
		Preload("Sender").
		Preload("Receiver").
		Where("sender_id = ? OR receiver_id = ?", userID, userID)
	q = applyTransactionFilters(q, f)
	q = applyCursor(q, f)
	err := q.Order("created_at desc, id desc").Limit(f.Limit + 1).Find(&txns).Error
	return txns, err
}

func (r *Repository) ListAllTransactions(f dto.TransactionFilter) ([]models.Transaction, error) {
	var txns []models.Transaction
	q := r.db.Model(&models.Transaction{}).
		Preload("Sender").
		Preload("Receiver")
	q = applyTransactionFilters(q, f)
	q = applyCursor(q, f)
	err := q.Order("created_at desc, id desc").Limit(f.Limit + 1).Find(&txns).Error
	return txns, err
}

func applyTransactionFilters(q *gorm.DB, f dto.TransactionFilter) *gorm.DB {
	if f.Type != "" {
		q = q.Where("type = ?", f.Type)
	}
	if f.Status != "" {
		q = q.Where("status = ?", f.Status)
	}
	return q
}

func (r *Repository) UpsertKYC(kyc *models.KYCVerification) error {
	return r.db.Clauses(clause.OnConflict{
		Columns:   []clause.Column{{Name: "user_id"}},
		DoUpdates: clause.AssignmentColumns([]string{"full_name", "dob", "address", "id_card_image_url", "selfie_image_url", "status", "rejection_reason", "updated_at"}),
	}).Create(kyc).Error
}

func (r *Repository) FindKYCByUserID(userID uuid.UUID) (*models.KYCVerification, error) {
	var kyc models.KYCVerification
	err := r.db.First(&kyc, "user_id = ?", userID).Error
	return &kyc, err
}

func (r *Repository) UpdateKYC(kyc *models.KYCVerification) error {
	return r.db.Save(kyc).Error
}

func (r *Repository) CreateRefreshToken(token *models.RefreshToken) error {
	return r.db.Create(token).Error
}

func (r *Repository) FindRefreshTokenByHash(hash string) (*models.RefreshToken, error) {
	var token models.RefreshToken
	err := r.db.First(&token, "token_hash = ?", hash).Error
	return &token, err
}

func (r *Repository) UpdateRefreshToken(token *models.RefreshToken) error {
	return r.db.Save(token).Error
}

func (r *Repository) RevokeUserRefreshTokens(userID uuid.UUID) error {
	return r.db.Model(&models.RefreshToken{}).
		Where("user_id = ? AND revoked_at IS NULL", userID).
		Update("revoked_at", gorm.Expr("NOW()")).Error
}

func (r *Repository) CreateAuditLog(log *models.AuditLog) error {
	return r.db.Create(log).Error
}

func (r *Repository) CreateQRToken(token *models.QRToken) error {
	return r.db.Create(token).Error
}

func (r *Repository) FindQRTokenByToken(tokenStr string) (*models.QRToken, error) {
	var token models.QRToken
	err := r.db.First(&token, "token = ?", tokenStr).Error
	return &token, err
}

func (r *Repository) MarkQRTokenUsed(id uuid.UUID) error {
	return r.db.Model(&models.QRToken{}).Where("id = ?", id).Update("is_used", true).Error
}

func (r *Repository) UpdateUserFCMToken(userID uuid.UUID, fcmToken string) error {
	return r.db.Model(&models.User{}).Where("id = ?", userID).Update("fcm_token", fcmToken).Error
}

func (r *Repository) GetUserFCMToken(userID uuid.UUID) (string, error) {
	var user models.User
	err := r.db.Select("fcm_token").First(&user, "id = ?", userID).Error
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return "", ErrNotFound
		}
		return "", err
	}
	return user.FCMToken, nil
}

func (r *Repository) CreateNotification(notification *models.Notification) error {
	return r.db.Create(notification).Error
}

func (r *Repository) MarkNotificationAsPushed(notificationID uuid.UUID) error {
	return r.db.Model(&models.Notification{}).
		Where("id = ?", notificationID).
		Update("is_pushed", true).Error
}

func (r *Repository) GetNotificationsByUserID(userID uuid.UUID, limit int) ([]models.Notification, error) {
	var notifications []models.Notification
	err := r.db.Where("user_id = ?", userID).
		Order("created_at DESC").
		Limit(limit).
		Find(&notifications).Error
	return notifications, err
}

func (r *Repository) MarkNotificationAsRead(notificationID uuid.UUID) error {
	return r.db.Model(&models.Notification{}).
		Where("id = ?", notificationID).
		Update("is_read", true).Error
}
