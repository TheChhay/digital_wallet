package main

import (
	"context"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"digital_wallet_api/internal/config"
	"digital_wallet_api/internal/handlers"
	"digital_wallet_api/internal/repositories"
	"digital_wallet_api/internal/routes"
	"digital_wallet_api/internal/services"
	"digital_wallet_api/internal/storage"
	"go.uber.org/zap"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		panic(err)
	}
	logger, err := config.NewLogger(cfg.AppEnv)
	if err != nil {
		panic(err)
	}
	defer logger.Sync()

	db, err := config.ConnectDB(cfg.DatabaseURL)
	if err != nil {
		logger.Fatal("database connection failed", zap.Error(err))
	}
	sqlDB, _ := db.DB()
	defer sqlDB.Close()

	repo := repositories.New(db)
	service := services.New(repo, cfg)
	
	// Initialize Firebase if credentials are configured
	if cfg.FirebaseCredentialsPath != "" && cfg.FirebaseProjectID != "" {
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		firebaseClient, err := config.InitFirebase(ctx, cfg)
		cancel()
		if err != nil {
			logger.Warn("Firebase initialization failed", zap.Error(err))
		} else {
			defer firebaseClient.Close()
			notificationService := services.NewNotificationService(firebaseClient.Messaging, firebaseClient.ProjectID, logger)
			service.SetNotificationService(notificationService)
			logger.Info("Firebase Messaging initialized successfully")
		}
	} else {
		logger.Warn("Firebase credentials not configured; notifications will be disabled")
	}

	store := storage.New(cfg)
	handler := handlers.New(service, store)
	router := routes.Setup(cfg, logger, handler)

	server := &http.Server{
		Addr:         ":" + cfg.HTTPPort,
		Handler:      router,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	go func() {
		logger.Info("api server starting", zap.String("addr", server.Addr))
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Fatal("server failed", zap.Error(err))
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := server.Shutdown(ctx); err != nil {
		logger.Error("graceful shutdown failed", zap.Error(err))
	}
	logger.Info("server stopped")
}
