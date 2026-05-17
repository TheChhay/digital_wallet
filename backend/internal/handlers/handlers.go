package handlers

import (
	"errors"
	"net/http"
	"strings"

	"digital_wallet_api/internal/dto"
	"digital_wallet_api/internal/middleware"
	"digital_wallet_api/internal/repositories"
	"digital_wallet_api/internal/services"
	"digital_wallet_api/internal/storage"
	"digital_wallet_api/internal/utils"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

type Handler struct {
	service *services.Service
	store   storage.Store
}

func New(service *services.Service, store storage.Store) *Handler {
	return &Handler{service: service, store: store}
}

func (h *Handler) Register(c *gin.Context) {
	var req dto.RegisterRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.Register(req)
	respond(c, http.StatusCreated, "User registered", resp, err)
}

func (h *Handler) Login(c *gin.Context) {
	var req dto.LoginRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.Login(req)
	respond(c, http.StatusOK, "Login successful", resp, err)
}

func (h *Handler) AdminLogin(c *gin.Context) {
	var req dto.LoginRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.AdminLogin(req)
	respond(c, http.StatusOK, "Admin login successful", resp, err)
}

func (h *Handler) Refresh(c *gin.Context) {
	var req dto.RefreshRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.Refresh(req.RefreshToken)
	respond(c, http.StatusOK, "Token refreshed", resp, err)
}

func (h *Handler) Logout(c *gin.Context) {
	userID := middleware.CurrentUserID(c)
	err := h.service.Logout(userID)
	respond(c, http.StatusOK, "Logged out", gin.H{}, err)
}

func (h *Handler) Profile(c *gin.Context) {
	resp, err := h.service.GetProfile(middleware.CurrentUserID(c))
	respond(c, http.StatusOK, "Success", resp, err)
}

func (h *Handler) UpdateProfileImage(c *gin.Context) {
	if isMultipart(c) {
		imageURL, ok := h.uploadImage(c, "image", "profile")
		if !ok {
			return
		}
		resp, err := h.service.UpdateProfileImage(middleware.CurrentUserID(c), imageURL)
		respond(c, http.StatusOK, "Profile image updated", resp, err)
		return
	}

	var req struct {
		ProfileImageURL string `json:"profile_image_url" binding:"required,url"`
	}

	if !bind(c, &req) {
		return
	}
	resp, err := h.service.UpdateProfileImage(middleware.CurrentUserID(c), req.ProfileImageURL)
	respond(c, http.StatusOK, "Profile image updated", resp, err)
}

func (h *Handler) SubmitKYC(c *gin.Context) {
	if isMultipart(c) {
		req := dto.KYCSubmitRequest{
			FullName: strings.TrimSpace(c.PostForm("full_name")),
			DOB:      strings.TrimSpace(c.PostForm("dob")),
			Address:  strings.TrimSpace(c.PostForm("address")),
		}
		if req.FullName == "" || req.DOB == "" || len(req.Address) < 10 {
			utils.Error(c, http.StatusBadRequest, "Invalid request", "full_name, dob, and address are required")
			return
		}
		idCardURL, ok := h.uploadImage(c, "id_card_image", "kyc/id-cards")
		if !ok {
			return
		}
		selfieURL, ok := h.uploadImage(c, "selfie_image", "kyc/selfies")
		if !ok {
			return
		}
		req.IDCardImageURL = idCardURL
		req.SelfieImageURL = selfieURL
		resp, err := h.service.SubmitKYC(middleware.CurrentUserID(c), req)
		respond(c, http.StatusOK, "KYC submitted", resp, err)
		return
	}

	var req dto.KYCSubmitRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.SubmitKYC(middleware.CurrentUserID(c), req)
	respond(c, http.StatusOK, "KYC submitted", resp, err)
}

func (h *Handler) Wallet(c *gin.Context) {
	resp, err := h.service.Wallet(middleware.CurrentUserID(c))
	respond(c, http.StatusOK, "Success", resp, err)
}

func (h *Handler) GetUserInfoByWalletID(c *gin.Context) {
	walletID := c.Query("wallet_id")
	if walletID == "" {
		walletID = c.Query("address")
	}
	if walletID == "" {
		utils.Error(c, http.StatusBadRequest, "wallet_id is required", nil)
		return
	}

	id, err := uuid.Parse(walletID)
	if err != nil {
		utils.Error(c, http.StatusBadRequest, "invalid wallet_id", nil)
		return
	}

	resp, err := h.service.GetUserInfoByWalletID(id)
	respond(c, http.StatusOK, "Success", resp, err)
}

func (h *Handler) Deposit(c *gin.Context) {
	var req dto.MoneyRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.Deposit(middleware.CurrentUserID(c), req)
	respond(c, http.StatusCreated, "Deposit successful", resp, err)
}

func (h *Handler) Withdraw(c *gin.Context) {
	var req dto.MoneyRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.Withdraw(middleware.CurrentUserID(c), req)
	respond(c, http.StatusCreated, "Withdraw successful", resp, err)
}

func (h *Handler) Transfer(c *gin.Context) {
	var req dto.TransferRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.Transfer(middleware.CurrentUserID(c), req)
	if errors.Is(err, services.ErrDuplicateTransfer) {
		utils.Success(c, http.StatusOK, "Duplicate request ignored; returning original transaction", resp)
		return
	}
	respond(c, http.StatusCreated, "Transfer successful", resp, err)
}

func (h *Handler) MyTransactions(c *gin.Context) {
	resp, err := h.service.UserTransactions(middleware.CurrentUserID(c), transactionFilter(c))
	respond(c, http.StatusOK, "Success", resp, err)
}

func (h *Handler) AdminUsers(c *gin.Context) {
	resp, err := h.service.ListUsers(cursorFilter(c))
	respond(c, http.StatusOK, "Success", resp, err)
}

func (h *Handler) AdminTransactions(c *gin.Context) {
	resp, err := h.service.AllTransactions(transactionFilter(c))
	respond(c, http.StatusOK, "Success", resp, err)
}

func (h *Handler) ReviewKYC(c *gin.Context) {
	var req dto.KYCReviewRequest
	if !bind(c, &req) {
		return
	}
	userID, err := uuid.Parse(c.Param("userID"))
	if err != nil {
		utils.Error(c, http.StatusBadRequest, "Invalid user id", nil)
		return
	}
	resp, err := h.service.ReviewKYC(middleware.CurrentUserID(c), userID, req)
	respond(c, http.StatusOK, "KYC reviewed", resp, err)
}

func (h *Handler) SetAccountStatus(c *gin.Context) {
	var req dto.AccountStatusRequest
	if !bind(c, &req) {
		return
	}
	userID, err := uuid.Parse(c.Param("userID"))
	if err != nil {
		utils.Error(c, http.StatusBadRequest, "Invalid user id", nil)
		return
	}
	resp, err := h.service.SetAccountStatus(userID, req.Status)
	respond(c, http.StatusOK, "Account status updated", resp, err)
}

func (h *Handler) GenerateQR(c *gin.Context) {
	var req dto.GenerateQRRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.GenerateQR(req)
	respond(c, http.StatusCreated, "QR Token generated", resp, err)
}

func (h *Handler) ValidateQR(c *gin.Context) {
	var req dto.ValidateTokenRequest
	if !bind(c, &req) {
		return
	}
	resp, err := h.service.ValidateQR(req)
	respond(c, http.StatusOK, "Token validated", resp, err)
}

func (h *Handler) GetStaticQR(c *gin.Context) {
	walletID := c.Query("address")
	if walletID == "" {
		utils.Error(c, http.StatusBadRequest, "address (wallet_id) is required", nil)
		return
	}
	resp, err := h.service.GenerateStaticQR(walletID)
	respond(c, http.StatusOK, "Static QR generated", resp, err)
}

func bind(c *gin.Context, req interface{}) bool {
	if err := c.ShouldBindJSON(req); err != nil {
		utils.BadRequest(c, err)
		return false
	}
	return true
}

func isMultipart(c *gin.Context) bool {
	return strings.HasPrefix(c.GetHeader("Content-Type"), "multipart/form-data")
}

func (h *Handler) uploadImage(c *gin.Context, field, folder string) (string, bool) {
	file, header, err := c.Request.FormFile(field)
	if err != nil && field == "image" {
		file, header, err = c.Request.FormFile("profile_image")
	}
	if err != nil {
		utils.BadRequest(c, err)
		return "", false
	}
	defer file.Close()
	url, err := h.store.UploadImage(c.Request.Context(), file, header, folder)
	if err != nil {
		utils.BadRequest(c, err)
		return "", false
	}
	return url, true
}

func respond(c *gin.Context, status int, message string, data interface{}, err error) {
	if err == nil {
		utils.Success(c, status, message, data)
		return
	}
	switch {
	case errors.Is(err, services.ErrInvalidCredentials):
		utils.Error(c, http.StatusUnauthorized, err.Error(), nil)
	case errors.Is(err, services.ErrForbidden):
		utils.Error(c, http.StatusForbidden, err.Error(), nil)
	case errors.Is(err, services.ErrFrozenAccount):
		utils.Error(c, http.StatusForbidden, err.Error(), nil)
	case errors.Is(err, services.ErrInsufficientFunds):
		utils.Error(c, http.StatusUnprocessableEntity, err.Error(), nil)
	case errors.Is(err, services.ErrInvalidCursor), errors.Is(err, services.ErrSelfTransfer), errors.Is(err, services.ErrTransferToAdmin):
		utils.Error(c, http.StatusBadRequest, err.Error(), nil)
	case errors.Is(err, repositories.ErrNotFound), errors.Is(err, gorm.ErrRecordNotFound):
		utils.Error(c, http.StatusNotFound, "Resource not found", nil)
	default:
		utils.Error(c, http.StatusInternalServerError, "Internal server error", err.Error())
	}
}

func transactionFilter(c *gin.Context) dto.TransactionFilter {
	f := cursorFilter(c)
	f.Type = c.Query("type")
	f.Status = c.Query("status")
	return f
}

func cursorFilter(c *gin.Context) dto.TransactionFilter {
	var f dto.TransactionFilter
	if err := c.ShouldBindQuery(&f); err != nil {
		return dto.TransactionFilter{Limit: 20}
	}
	return f
}
