package config

import (
	"fmt"
	"os"
	"strconv"
	"time"

	"github.com/joho/godotenv"
)

type Config struct {
	AppEnv          string
	HTTPPort        string
	DatabaseURL     string
	JWTSecret       string
	AccessTokenTTL  time.Duration
	RefreshTokenTTL time.Duration
	RateLimitRPS    float64
	RateLimitBurst  int
	UploadDir       string
	R2AccountID     string
	R2AccessKeyID   string
	R2SecretKey     string
	R2Bucket        string
	R2Endpoint      string
	R2PublicBaseURL string
	R2Region        string
}

func Load() (*Config, error) {
	_ = godotenv.Load()
	cfg := &Config{
		AppEnv:          getEnv("APP_ENV", "development"),
		HTTPPort:        getEnv("HTTP_PORT", "8080"),
		DatabaseURL:     getEnv("DATABASE_URL", ""),
		JWTSecret:       getEnv("JWT_SECRET", ""),
		AccessTokenTTL:  durationMinutes("ACCESS_TOKEN_TTL_MINUTES", 15),
		RefreshTokenTTL: durationHours("REFRESH_TOKEN_TTL_HOURS", 24*30),
		RateLimitRPS:    floatEnv("RATE_LIMIT_RPS", 5),
		RateLimitBurst:  intEnv("RATE_LIMIT_BURST", 20),
		UploadDir:       getEnv("UPLOAD_DIR", "upload"),
		R2AccountID:     getEnv("R2_ACCOUNT_ID", ""),
		R2AccessKeyID:   getEnv("R2_ACCESS_KEY_ID", ""),
		R2SecretKey:     getEnv("R2_SECRET_ACCESS_KEY", ""),
		R2Bucket:        getEnv("R2_BUCKET", ""),
		R2Endpoint:      getEnv("R2_ENDPOINT", ""),
		R2PublicBaseURL: getEnv("R2_PUBLIC_BASE_URL", ""),
		R2Region:        getEnv("R2_REGION", "auto"),
	}
	if cfg.DatabaseURL == "" {
		return nil, fmt.Errorf("DATABASE_URL is required")
	}
	if len(cfg.JWTSecret) < 32 {
		return nil, fmt.Errorf("JWT_SECRET must be at least 32 characters")
	}
	if cfg.AppEnv == "production" {
		if cfg.R2AccessKeyID == "" || cfg.R2SecretKey == "" || cfg.R2Bucket == "" || cfg.R2Endpoint == "" || cfg.R2PublicBaseURL == "" {
			return nil, fmt.Errorf("R2 configuration is required in production")
		}
	}
	return cfg, nil
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func intEnv(key string, fallback int) int {
	v, err := strconv.Atoi(getEnv(key, ""))
	if err != nil {
		return fallback
	}
	return v
}

func floatEnv(key string, fallback float64) float64 {
	v, err := strconv.ParseFloat(getEnv(key, ""), 64)
	if err != nil {
		return fallback
	}
	return v
}

func durationMinutes(key string, fallback int) time.Duration {
	return time.Duration(intEnv(key, fallback)) * time.Minute
}

func durationHours(key string, fallback int) time.Duration {
	return time.Duration(intEnv(key, fallback)) * time.Hour
}
