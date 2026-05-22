package services

import (
	"context"
	"fmt"

	"digital_wallet_api/internal/models"
	"firebase.google.com/go/v4/messaging"
	"go.uber.org/zap"
)

type NotificationService struct {
	messagingClient *messaging.Client
	logger          *zap.Logger
	projectID       string
}

func NewNotificationService(client *messaging.Client, projectID string, logger *zap.Logger) *NotificationService {
	return &NotificationService{
		messagingClient: client,
		logger:          logger,
		projectID:       projectID,
	}
}

type NotificationPayload struct {
	Title       string
	Message     string
	Type        models.NotificationType
	Amount      *float64
	SenderName  *string
	ReceiverName *string
}

func (ns *NotificationService) SendNotification(ctx context.Context, fcmToken string, payload NotificationPayload) (string, error) {
	if fcmToken == "" {
		return "", fmt.Errorf("FCM token is empty")
	}

	data := map[string]string{
		"type": string(payload.Type),
	}

	if payload.Amount != nil {
		data["amount"] = fmt.Sprintf("%.2f", *payload.Amount)
	}

	if payload.SenderName != nil {
		data["sender_name"] = *payload.SenderName
	}

	if payload.ReceiverName != nil {
		data["receiver_name"] = *payload.ReceiverName
	}

	message := &messaging.Message{
		Notification: &messaging.Notification{
			Title: payload.Title,
			Body:  payload.Message,
		},
		Data:  data,
		Token: fcmToken,
	}

	messageID, err := ns.messagingClient.Send(ctx, message)
	if err != nil {
		ns.logger.Error("Failed to send FCM notification", zap.Error(err), zap.String("fcm_token", fcmToken))
		return "", fmt.Errorf("failed to send FCM notification: %w", err)
	}

	ns.logger.Info("FCM notification sent successfully",
		zap.String("message_id", messageID),
		zap.String("type", string(payload.Type)),
	)

	return messageID, nil
}

func (ns *NotificationService) SendMulticast(ctx context.Context, fcmTokens []string, payload NotificationPayload) (*messaging.BatchResponse, error) {
	if len(fcmTokens) == 0 {
		return nil, fmt.Errorf("FCM tokens list is empty")
	}

	data := map[string]string{
		"type": string(payload.Type),
	}

	if payload.Amount != nil {
		data["amount"] = fmt.Sprintf("%.2f", *payload.Amount)
	}

	message := &messaging.MulticastMessage{
		Notification: &messaging.Notification{
			Title: payload.Title,
			Body:  payload.Message,
		},
		Data:   data,
		Tokens: fcmTokens,
	}

	response, err := ns.messagingClient.SendMulticast(ctx, message)
	if err != nil {
		ns.logger.Error("Failed to send multicast FCM notification", zap.Error(err), zap.Int("token_count", len(fcmTokens)))
		return nil, fmt.Errorf("failed to send multicast FCM notification: %w", err)
	}

	ns.logger.Info("Multicast FCM notification sent",
		zap.Int("success_count", response.SuccessCount),
		zap.Int("failure_count", response.FailureCount),
		zap.String("type", string(payload.Type)),
	)

	return response, nil
}
