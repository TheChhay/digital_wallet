package config

import (
	"context"
	"fmt"

	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"google.golang.org/api/option"
)

type FirebaseClient struct {
	App       *firebase.App
	Messaging *messaging.Client
	ProjectID string
}

func InitFirebase(ctx context.Context, cfg *Config) (*FirebaseClient, error) {
	if cfg.FirebaseCredentialsPath == "" || cfg.FirebaseProjectID == "" {
		return nil, fmt.Errorf("Firebase credentials path and project ID are required")
	}

	opt := option.WithCredentialsFile(cfg.FirebaseCredentialsPath)
	app, err := firebase.NewApp(ctx, nil, opt)
	if err != nil {
		return nil, fmt.Errorf("error initializing Firebase app: %w", err)
	}

	client, err := app.Messaging(ctx)
	if err != nil {
		return nil, fmt.Errorf("error initializing Firebase Messaging client: %w", err)
	}

	return &FirebaseClient{
		App:       app,
		Messaging: client,
		ProjectID: cfg.FirebaseProjectID,
	}, nil
}

func (fc *FirebaseClient) Close() error {
	// Firebase SDK clients don't require explicit closing
	return nil
}
