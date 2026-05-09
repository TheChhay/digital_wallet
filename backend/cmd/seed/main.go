package main

import (
	"fmt"
	"log"

	"digital_wallet_api/internal/config"
	"digital_wallet_api/internal/models"
	"digital_wallet_api/internal/utils"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

const seedPassword = "Password123!"

type seedUser struct {
	Phone        string
	FullName     string
	Role         models.Role
	BalanceCents int64
}

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatal(err)
	}

	db, err := config.ConnectDB(cfg.DatabaseURL)
	if err != nil {
		log.Fatal(err)
	}
	sqlDB, _ := db.DB()
	defer sqlDB.Close()

	users := []seedUser{
		{Phone: "+85510000001", FullName: "Seed Admin", Role: models.RoleAdmin, BalanceCents: 0},
		{Phone: "+85510000002", FullName: "Seed User A", Role: models.RoleUser, BalanceCents: 100000},
		{Phone: "+85510000003", FullName: "Seed User B", Role: models.RoleUser, BalanceCents: 50000},
	}

	for _, item := range users {
		user, err := upsertUser(db, item)
		if err != nil {
			log.Fatal(err)
		}
		fmt.Printf("%-12s phone=%s password=%s balance_cents=%d\n", item.Role, user.Phone, seedPassword, item.BalanceCents)
	}
}

func upsertUser(db *gorm.DB, item seedUser) (*models.User, error) {
	passwordHash, err := utils.HashPassword(seedPassword)
	if err != nil {
		return nil, err
	}

	var user models.User
	err = db.Transaction(func(tx *gorm.DB) error {
		user = models.User{
			Phone:        item.Phone,
			PasswordHash: passwordHash,
			FullName:     item.FullName,
			Role:         item.Role,
			Status:       models.UserActive,
		}
		err := tx.Clauses(clause.OnConflict{
			Columns: []clause.Column{{Name: "phone"}},
			DoUpdates: clause.AssignmentColumns([]string{
				"password_hash",
				"full_name",
				"role",
				"status",
				"updated_at",
			}),
		}).Create(&user).Error
		if err != nil {
			return err
		}
		if err := tx.Where("phone = ?", item.Phone).First(&user).Error; err != nil {
			return err
		}

		wallet := models.Wallet{UserID: user.ID, BalanceCents: item.BalanceCents}
		return tx.Clauses(clause.OnConflict{
			Columns:   []clause.Column{{Name: "user_id"}},
			DoUpdates: clause.AssignmentColumns([]string{"balance_cents", "updated_at"}),
		}).Create(&wallet).Error
	})
	if err != nil {
		return nil, err
	}
	return &user, nil
}
