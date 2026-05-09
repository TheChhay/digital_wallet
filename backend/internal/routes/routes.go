package routes

import (
	"net/http"

	"digital_wallet_api/internal/config"
	"digital_wallet_api/internal/handlers"
	"digital_wallet_api/internal/middleware"
	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

func Setup(cfg *config.Config, logger *zap.Logger, h *handlers.Handler) *gin.Engine {
	if cfg.AppEnv == "production" {
		gin.SetMode(gin.ReleaseMode)
	}
	r := gin.New()
	r.MaxMultipartMemory = 8 << 20
	r.Use(middleware.Recovery(logger), middleware.Logger(logger), middleware.RateLimit(cfg))

	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"success": true, "message": "ok"})
	})
	r.GET("/openapi.yaml", func(c *gin.Context) {
		c.File("docs/openapi.yaml")
	})
	r.GET("/swagger", swaggerUI)
	r.GET("/swagger/", swaggerUI)
	r.GET("/swagger/index.html", swaggerUI)
	if cfg.AppEnv != "production" {
		r.Static("/uploads", cfg.UploadDir)
	}

	v1 := r.Group("/api/v1")
	{
		auth := v1.Group("/auth")
		auth.POST("/register", h.Register)
		auth.POST("/login", h.Login)
		auth.POST("/refresh", h.Refresh)
		auth.POST("/logout", middleware.Auth(cfg), h.Logout)

		me := v1.Group("/me", middleware.Auth(cfg))
		me.GET("", h.Profile)
		me.PUT("/profile-image", h.UpdateProfileImage)
		me.POST("/kyc", h.SubmitKYC)

		wallet := v1.Group("/wallet", middleware.Auth(cfg))
		wallet.GET("", h.Wallet)
		wallet.POST("/deposit", h.Deposit)
		wallet.POST("/withdraw", h.Withdraw)
		wallet.POST("/transfer", h.Transfer)
		wallet.GET("/transactions", h.MyTransactions)

		admin := v1.Group("/admin")
		admin.POST("/login", h.AdminLogin)
		admin.Use(middleware.Auth(cfg), middleware.RequireAdmin())
		admin.GET("/users", h.AdminUsers)
		admin.GET("/transactions", h.AdminTransactions)
		admin.PUT("/users/:userID/kyc", h.ReviewKYC)
		admin.PUT("/users/:userID/status", h.SetAccountStatus)
	}
	return r
}

func swaggerUI(c *gin.Context) {
	c.Header("Content-Type", "text/html; charset=utf-8")
	c.String(http.StatusOK, `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Digital Wallet API Docs</title>
  <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
</head>
<body>
  <div id="swagger-ui"></div>
  <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
  <script>
    window.onload = function() {
      window.ui = SwaggerUIBundle({
        url: "/openapi.yaml",
        dom_id: "#swagger-ui"
      });
    };
  </script>
</body>
</html>`)
}
