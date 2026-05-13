package services

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"digital_wallet_api/internal/config"
	"digital_wallet_api/internal/dto"
	"digital_wallet_api/internal/models"
	"digital_wallet_api/internal/repositories"
	"digital_wallet_api/internal/utils"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

var (
	ErrInvalidCredentials = errors.New("invalid phone or password")
	ErrFrozenAccount      = errors.New("account is frozen")
	ErrInsufficientFunds  = errors.New("insufficient funds")
	ErrDuplicateTransfer  = errors.New("duplicate transfer request")
	ErrForbidden          = errors.New("forbidden")
	ErrInvalidCursor      = errors.New("invalid cursor")
)

type Service struct {
	repo *repositories.Repository
	cfg  *config.Config
}

func New(repo *repositories.Repository, cfg *config.Config) *Service {
	return &Service{repo: repo, cfg: cfg}
}

func (s *Service) Register(req dto.RegisterRequest) (*dto.AuthResponse, error) {
	passwordHash, err := utils.HashPassword(req.Password)
	if err != nil {
		return nil, err
	}
	user := &models.User{
		Phone:        req.Phone,
		PasswordHash: passwordHash,
		FirstName:    req.FirstName,
		LastName:     req.LastName,
		Role:         models.RoleUser,
		Status:       models.UserActive,
	}
	err = s.repo.DB().Transaction(func(tx *gorm.DB) error {
		txRepo := s.repo.WithTx(tx)
		if err := txRepo.CreateUser(user); err != nil {
			return err
		}
		return txRepo.CreateWallet(&models.Wallet{UserID: user.ID, BalanceCents: 0})
	})
	if err != nil {
		return nil, err
	}
	return s.issueTokens(user)
}

func (s *Service) Login(req dto.LoginRequest) (*dto.AuthResponse, error) {
	user, err := s.repo.FindUserByPhone(req.Phone)
	if err != nil || utils.CheckPassword(user.PasswordHash, req.Password) != nil {
		return nil, ErrInvalidCredentials
	}
	if user.Status == models.UserFrozen {
		return nil, ErrFrozenAccount
	}
	return s.issueTokens(user)
}

func (s *Service) AdminLogin(req dto.LoginRequest) (*dto.AuthResponse, error) {
	resp, err := s.Login(req)
	if err != nil {
		return nil, err
	}
	if resp.User.Role != string(models.RoleAdmin) {
		return nil, ErrForbidden
	}
	return resp, nil
}

func (s *Service) Refresh(refreshToken string) (*dto.AuthResponse, error) {
	hash := utils.HashToken(refreshToken)
	stored, err := s.repo.FindRefreshTokenByHash(hash)
	if err != nil || stored.RevokedAt != nil || stored.ExpiresAt.Before(time.Now()) {
		return nil, ErrInvalidCredentials
	}
	user, err := s.repo.FindUserByID(stored.UserID)
	if err != nil {
		return nil, err
	}
	var response *dto.AuthResponse
	err = s.repo.DB().Transaction(func(tx *gorm.DB) error {
		txRepo := s.repo.WithTx(tx)
		now := time.Now()
		stored.RevokedAt = &now
		newRefresh, err := utils.RandomToken()
		if err != nil {
			return err
		}
		newHash := utils.HashToken(newRefresh)
		stored.ReplacedByTokenHash = newHash
		if err := txRepo.UpdateRefreshToken(stored); err != nil {
			return err
		}
		if err := txRepo.CreateRefreshToken(&models.RefreshToken{
			UserID:    user.ID,
			TokenHash: newHash,
			ExpiresAt: time.Now().Add(s.cfg.RefreshTokenTTL),
		}); err != nil {
			return err
		}
		access, err := utils.GenerateAccessToken(s.cfg.JWTSecret, user.ID, string(user.Role), s.cfg.AccessTokenTTL)
		if err != nil {
			return err
		}
		response = &dto.AuthResponse{AccessToken: access, RefreshToken: newRefresh, User: mapUser(user)}
		return nil
	})
	return response, err
}

func (s *Service) Logout(userID uuid.UUID) error {
	return s.repo.RevokeUserRefreshTokens(userID)
}

func (s *Service) GetProfile(userID uuid.UUID) (*dto.UserResponse, error) {
	user, err := s.repo.FindUserByID(userID)
	if err != nil {
		return nil, err
	}
	resp := mapUser(user)
	return &resp, nil
}

func (s *Service) UpdateProfileImage(userID uuid.UUID, imageURL string) (*dto.UserResponse, error) {
	user, err := s.repo.FindUserByID(userID)
	if err != nil {
		return nil, err
	}
	user.ProfileImageURL = imageURL
	if err := s.repo.UpdateUser(user); err != nil {
		return nil, err
	}
	resp := mapUser(user)
	return &resp, nil
}

func (s *Service) SubmitKYC(userID uuid.UUID, req dto.KYCSubmitRequest) (*models.KYCVerification, error) {
	dob, err := time.Parse("2006-01-02", req.DOB)
	if err != nil {
		return nil, fmt.Errorf("dob must use YYYY-MM-DD")
	}
	kyc := &models.KYCVerification{
		UserID:         userID,
		FullName:       req.FullName,
		DOB:            dob,
		Address:        req.Address,
		IDCardImageURL: req.IDCardImageURL,
		SelfieImageURL: req.SelfieImageURL,
		Status:         models.KYCPending,
	}
	if err := s.repo.UpsertKYC(kyc); err != nil {
		return nil, err
	}
	return s.repo.FindKYCByUserID(userID)
}

func (s *Service) ReviewKYC(adminID, userID uuid.UUID, req dto.KYCReviewRequest) (*models.KYCVerification, error) {
	kyc, err := s.repo.FindKYCByUserID(userID)
	if err != nil {
		return nil, err
	}
	now := time.Now()
	kyc.Status = models.KYCStatus(req.Status)
	kyc.RejectionReason = req.RejectionReason
	kyc.ReviewedBy = &adminID
	kyc.ReviewedAt = &now
	if err := s.repo.UpdateKYC(kyc); err != nil {
		return nil, err
	}
	return kyc, nil
}

func (s *Service) Wallet(userID uuid.UUID) (*models.Wallet, error) {
	return s.repo.GetWalletByUserID(userID)
}

func (s *Service) Deposit(userID uuid.UUID, req dto.MoneyRequest) (*models.Transaction, error) {
	return s.moneyMovement(nil, &userID, models.TransactionDeposit, req.AmountCents, req.Description)
}

func (s *Service) Withdraw(userID uuid.UUID, req dto.MoneyRequest) (*models.Transaction, error) {
	return s.moneyMovement(&userID, nil, models.TransactionWithdraw, req.AmountCents, req.Description)
}

func (s *Service) Transfer(senderID uuid.UUID, req dto.TransferRequest) (*models.Transaction, error) {
	if existing, err := s.repo.FindTransactionByIdempotencyKey(senderID, req.IdempotencyKey); err == nil {
		return existing, ErrDuplicateTransfer
	}
	receiver, err := s.repo.FindUserByPhone(req.ReceiverPhone)
	if err != nil {
		return nil, err
	}
	if receiver.ID == senderID {
		return nil, fmt.Errorf("cannot transfer to yourself")
	}
	if receiver.Status == models.UserFrozen {
		return nil, ErrFrozenAccount
	}
	var txn *models.Transaction
	err = s.repo.DB().Transaction(func(tx *gorm.DB) error {
		txRepo := s.repo.WithTx(tx)
		first, second := senderID, receiver.ID
		if first.String() > second.String() {
			first, second = second, first
		}
		firstWallet, err := txRepo.GetWalletForUpdate(first)
		if err != nil {
			return err
		}
		secondWallet, err := txRepo.GetWalletForUpdate(second)
		if err != nil {
			return err
		}
		var senderWallet, receiverWallet *models.Wallet
		if firstWallet.UserID == senderID {
			senderWallet, receiverWallet = firstWallet, secondWallet
		} else {
			senderWallet, receiverWallet = secondWallet, firstWallet
		}
		if senderWallet.BalanceCents < req.AmountCents {
			return ErrInsufficientFunds
		}
		senderWallet.BalanceCents -= req.AmountCents
		receiverWallet.BalanceCents += req.AmountCents
		if err := txRepo.UpdateWallet(senderWallet); err != nil {
			return err
		}
		if err := txRepo.UpdateWallet(receiverWallet); err != nil {
			return err
		}
		txn = &models.Transaction{
			Reference:      utils.NewReference("TRF"),
			IdempotencyKey: req.IdempotencyKey,
			Type:           models.TransactionTransfer,
			Status:         models.TransactionSuccess,
			AmountCents:    req.AmountCents,
			SenderID:       &senderID,
			ReceiverID:     &receiver.ID,
			Description:    req.Description,
		}
		return txRepo.CreateTransaction(txn)
	})
	return txn, err
}

func (s *Service) moneyMovement(senderID, receiverID *uuid.UUID, txType models.TransactionType, amount int64, desc string) (*models.Transaction, error) {
	var txn *models.Transaction
	err := s.repo.DB().Transaction(func(tx *gorm.DB) error {
		txRepo := s.repo.WithTx(tx)
		var wallet *models.Wallet
		var err error
		if senderID != nil {
			wallet, err = txRepo.GetWalletForUpdate(*senderID)
			if err != nil {
				return err
			}
			if wallet.BalanceCents < amount {
				return ErrInsufficientFunds
			}
			wallet.BalanceCents -= amount
		}
		if receiverID != nil {
			wallet, err = txRepo.GetWalletForUpdate(*receiverID)
			if err != nil {
				return err
			}
			wallet.BalanceCents += amount
		}
		if err := txRepo.UpdateWallet(wallet); err != nil {
			return err
		}
		prefix := map[models.TransactionType]string{models.TransactionDeposit: "DEP", models.TransactionWithdraw: "WDR"}[txType]
		txn = &models.Transaction{
			Reference:   utils.NewReference(prefix),
			Type:        txType,
			Status:      models.TransactionSuccess,
			AmountCents: amount,
			SenderID:    senderID,
			ReceiverID:  receiverID,
			Description: desc,
		}
		return txRepo.CreateTransaction(txn)
	})
	return txn, err
}

func (s *Service) UserTransactions(userID uuid.UUID, f dto.TransactionFilter) (*dto.CursorResponse, error) {
	if err := normalizeCursor(&f); err != nil {
		return nil, err
	}
	items, err := s.repo.ListUserTransactions(userID, f)
	if err != nil {
		return nil, err
	}
	return paginateTransactions(items, f.Limit), nil
}

func (s *Service) AllTransactions(f dto.TransactionFilter) (*dto.CursorResponse, error) {
	if err := normalizeCursor(&f); err != nil {
		return nil, err
	}
	items, err := s.repo.ListAllTransactions(f)
	if err != nil {
		return nil, err
	}
	return paginateTransactions(items, f.Limit), nil
}

func (s *Service) ListUsers(f dto.TransactionFilter) (*dto.CursorResponse, error) {
	if err := normalizeCursor(&f); err != nil {
		return nil, err
	}
	items, err := s.repo.ListUsers(f)
	if err != nil {
		return nil, err
	}
	return paginateUsers(items, f.Limit), nil
}

func (s *Service) SetAccountStatus(userID uuid.UUID, status string) (*dto.UserResponse, error) {
	user, err := s.repo.FindUserByID(userID)
	if err != nil {
		return nil, err
	}
	user.Status = models.UserStatus(status)
	if err := s.repo.UpdateUser(user); err != nil {
		return nil, err
	}
	resp := mapUser(user)
	return &resp, nil
}

func (s *Service) Audit(actorID *uuid.UUID, action, entity string, entityID *uuid.UUID, ip, ua string) {
	_ = s.repo.CreateAuditLog(&models.AuditLog{
		ActorID: actorID, Action: action, Entity: entity, EntityID: entityID, IPAddress: ip, UserAgent: ua, Metadata: "{}",
	})
}

func (s *Service) issueTokens(user *models.User) (*dto.AuthResponse, error) {
	access, err := utils.GenerateAccessToken(s.cfg.JWTSecret, user.ID, string(user.Role), s.cfg.AccessTokenTTL)
	if err != nil {
		return nil, err
	}
	refresh, err := utils.RandomToken()
	if err != nil {
		return nil, err
	}
	if err := s.repo.CreateRefreshToken(&models.RefreshToken{
		UserID:    user.ID,
		TokenHash: utils.HashToken(refresh),
		ExpiresAt: time.Now().Add(s.cfg.RefreshTokenTTL),
	}); err != nil {
		return nil, err
	}
	return &dto.AuthResponse{AccessToken: access, RefreshToken: refresh, User: mapUser(user)}, nil
}

func mapUser(user *models.User) dto.UserResponse {
	return dto.UserResponse{
		ID:              user.ID,
		Phone:           user.Phone,
		FirstName:       user.FirstName,
		LastName:        user.LastName,
		Role:            string(user.Role),
		Status:          string(user.Status),
		ProfileImageURL: user.ProfileImageURL,
		CreatedAt:       user.CreatedAt,
	}
}

func normalizeCursor(f *dto.TransactionFilter) error {
	if f.Limit < 1 || f.Limit > 100 {
		f.Limit = 20
	}
	if f.Cursor == "" {
		return nil
	}
	cursor, err := decodeCursor(f.Cursor)
	if err != nil {
		return ErrInvalidCursor
	}
	f.CursorCreatedAt = cursor.CreatedAt
	f.CursorID = cursor.ID
	return nil
}

func paginateUsers(items []models.User, limit int) *dto.CursorResponse {
	hasMore := len(items) > limit
	if hasMore {
		items = items[:limit]
	}
	resp := &dto.CursorResponse{Items: items, Limit: limit, HasMore: hasMore}
	if hasMore && len(items) > 0 {
		last := items[len(items)-1]
		resp.NextCursor = encodeCursor(last.CreatedAt, last.ID)
	}
	return resp
}

func paginateTransactions(items []models.Transaction, limit int) *dto.CursorResponse {
	hasMore := len(items) > limit
	if hasMore {
		items = items[:limit]
	}
	resp := &dto.CursorResponse{Items: items, Limit: limit, HasMore: hasMore}
	if hasMore && len(items) > 0 {
		last := items[len(items)-1]
		resp.NextCursor = encodeCursor(last.CreatedAt, last.ID)
	}
	return resp
}

type cursorToken struct {
	CreatedAt time.Time `json:"created_at"`
	ID        uuid.UUID `json:"id"`
}

func encodeCursor(createdAt time.Time, id uuid.UUID) string {
	payload, _ := json.Marshal(cursorToken{CreatedAt: createdAt.UTC(), ID: id})
	return base64.RawURLEncoding.EncodeToString(payload)
}

func decodeCursor(token string) (cursorToken, error) {
	var cursor cursorToken
	payload, err := base64.RawURLEncoding.DecodeString(token)
	if err != nil {
		return cursor, err
	}
	if err := json.Unmarshal(payload, &cursor); err != nil {
		return cursor, err
	}
	if cursor.CreatedAt.IsZero() || cursor.ID == uuid.Nil {
		return cursor, fmt.Errorf("empty cursor")
	}
	return cursor, nil
}
