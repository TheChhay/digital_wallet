package middleware

import (
	"net/http"
	"strings"
	"time"

	"digital_wallet_api/internal/config"
	"digital_wallet_api/internal/models"
	"digital_wallet_api/internal/utils"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"go.uber.org/zap"
	"golang.org/x/time/rate"
)

const (
	ContextUserID = "user_id"
	ContextRole   = "role"
)

func Recovery(logger *zap.Logger) gin.HandlerFunc {
	return gin.CustomRecovery(func(c *gin.Context, recovered interface{}) {
		logger.Error("panic recovered", zap.Any("panic", recovered), zap.String("path", c.Request.URL.Path))
		utils.Error(c, http.StatusInternalServerError, "Internal server error", nil)
	})
}

func Logger(logger *zap.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		c.Next()
		logger.Info("http_request",
			zap.String("method", c.Request.Method),
			zap.String("path", c.Request.URL.Path),
			zap.Int("status", c.Writer.Status()),
			zap.Duration("latency", time.Since(start)),
			zap.String("ip", c.ClientIP()),
		)
	}
}

func RateLimit(cfg *config.Config) gin.HandlerFunc {
	limiter := rate.NewLimiter(rate.Limit(cfg.RateLimitRPS), cfg.RateLimitBurst)
	return func(c *gin.Context) {
		if !limiter.Allow() {
			utils.Error(c, http.StatusTooManyRequests, "Too many requests", nil)
			c.Abort()
			return
		}
		c.Next()
	}
}

func Auth(cfg *config.Config) gin.HandlerFunc {
	return func(c *gin.Context) {
		header := c.GetHeader("Authorization")
		if !strings.HasPrefix(header, "Bearer ") {
			utils.Error(c, http.StatusUnauthorized, "Missing bearer token", nil)
			c.Abort()
			return
		}
		claims, err := utils.ParseAccessToken(cfg.JWTSecret, strings.TrimPrefix(header, "Bearer "))
		if err != nil {
			zap.L().Error("JWT validation failed", zap.Error(err), zap.String("path", c.Request.URL.Path))
			utils.Error(c, http.StatusUnauthorized, "Invalid token", nil)
			c.Abort()
			return
		}
		c.Set(ContextUserID, claims.UserID)
		c.Set(ContextRole, claims.Role)
		c.Next()
	}
}

func RequireAdmin() gin.HandlerFunc {
	return func(c *gin.Context) {
		if role, _ := c.Get(ContextRole); role != string(models.RoleAdmin) {
			utils.Error(c, http.StatusForbidden, "Admin access required", nil)
			c.Abort()
			return
		}
		c.Next()
	}
}

func CurrentUserID(c *gin.Context) uuid.UUID {
	id, _ := c.Get(ContextUserID)
	if uid, ok := id.(uuid.UUID); ok {
		return uid
	}
	return uuid.Nil
}
