# Firebase Cloud Messaging (FCM) Implementation Guide

## Overview
Your Digital Wallet backend now has complete Firebase Cloud Messaging integration for sending push notifications to users' mobile devices when money is sent or received.

## Architecture

### Components Added
1. **Database Migration** - Added `fcm_token` column to `users` table
2. **Models** - Added `Notification` and `NotificationType` models
3. **Firebase Config** - `internal/config/firebase.go` for Firebase Admin SDK initialization
4. **Notification Service** - `internal/services/notification_service.go` for sending FCM messages
5. **Repository Methods** - Methods for managing FCM tokens and notifications
6. **API Endpoints** - Two new endpoints for FCM token management
7. **Transaction Integration** - Automatic notification sending on successful transfers

## Setup Instructions

### 1. Get Firebase Credentials

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to **Project Settings** → **Service Accounts**
4. Click **Generate New Private Key**
5. Save the JSON file securely (keep it secret!)

### 2. Configure Environment Variables

Add these to your `.env` file:

```bash
FIREBASE_CREDENTIALS_PATH=/path/to/your/firebase-key.json
FIREBASE_PROJECT_ID=your-project-id
```

Replace:
- `/path/to/your/firebase-key.json` - Full path to your Firebase service account key
- `your-project-id` - Your Firebase project ID (from the JSON file or console)

### 3. Run Database Migration

```bash
goose -dir migrations postgres "$DATABASE_URL" up
```

This adds the `fcm_token` column to the `users` table.

### 4. Rebuild and Deploy

```bash
go mod tidy
go build -o api ./cmd/api/main.go
./api
```

## API Endpoints

### 1. Register/Update FCM Token
**POST** `/api/v1/users/fcm-token`

**Authentication:** Required (Bearer token)

**Request Body:**
```json
{
  "fcm_token": "eydAbCd123...xyz"
}
```

**Response:**
```json
{
  "success": true,
  "message": "FCM token registered",
  "data": {}
}
```

**Usage (from Kotlin app):**
```kotlin
val token = FirebaseMessaging.getInstance().getToken().addOnSuccessListener { token ->
    // Send token to backend
    val request = RegisterFCMTokenRequest(fcm_token = token)
    // POST to /api/v1/users/fcm-token with this request
}
```

### 2. Get Notifications
**GET** `/api/v1/users/notifications`

**Authentication:** Required (Bearer token)

**Response:**
```json
{
  "success": true,
  "message": "Notifications fetched",
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "type": "MONEY_RECEIVED",
      "title": "Money Received",
      "message": "You received $50.00 from John Doe",
      "amount": 50.00,
      "related_tx_id": "550e8400-e29b-41d4-a716-446655440001",
      "is_read": false,
      "created_at": "2024-05-22T10:00:00Z"
    }
  ]
}
```

## Notification Flow

### When Money is Transferred

1. User A calls `/api/v1/wallet/transfer` endpoint
2. Transaction is created and marked as `success`
3. System retrieves FCM tokens for both users
4. **Receiver notification:**
   - Type: `MONEY_RECEIVED`
   - Title: "Money Received"
   - Message: "You received $X.XX from [Sender Name]"
5. **Sender notification:**
   - Type: `MONEY_SENT`
   - Title: "Transfer Successful"
   - Message: "You sent $X.XX to [Receiver Name]"
6. Both notifications are:
   - Stored in the `notifications` table
   - Sent via Firebase Cloud Messaging
   - Marked as `is_pushed: true` after successful delivery

### Notification Payload

All notifications include:
```json
{
  "notification": {
    "title": "Money Received",
    "body": "You received $50.00 from John Doe"
  },
  "data": {
    "type": "MONEY_RECEIVED",
    "amount": "50.00",
    "sender_name": "John Doe"
  }
}
```

## Integration with Kotlin App

### 1. Register for Push Notifications

```kotlin
import com.google.firebase.messaging.FirebaseMessaging

FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        // Send token to backend
        apiService.registerFCMToken(RegisterFCMTokenRequest(token))
    }
}
```

### 2. Handle Incoming Messages

```kotlin
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "New Notification"
        val body = remoteMessage.notification?.body ?: ""
        val type = remoteMessage.data["type"] ?: ""
        
        // Display notification to user
        showNotification(title, body, type)
    }
}
```

### 3. Update AndroidManifest.xml

```xml
<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## Database Schema Changes

### New Column on `users` Table
```sql
ALTER TABLE users ADD COLUMN fcm_token VARCHAR(500);
CREATE INDEX idx_users_fcm_token ON users(fcm_token) WHERE fcm_token IS NOT NULL;
```

### Notifications Table (Already Exists)
```
- id: UUID (primary key)
- user_id: UUID (foreign key → users.id)
- type: VARCHAR(50) - 'MONEY_RECEIVED' | 'MONEY_SENT'
- title: VARCHAR(255)
- message: TEXT
- amount: DECIMAL(15,2) - nullable
- related_tx_id: UUID - nullable (foreign key → transactions.id)
- is_read: BOOLEAN (default: false)
- is_pushed: BOOLEAN (default: false)
- created_at: TIMESTAMP
```

## Error Handling

### Firebase Not Configured
If Firebase credentials are not configured:
- The app logs a warning on startup
- Notifications are **disabled** (graceful degradation)
- API endpoints still work, but no FCM messages are sent
- Notifications are still stored in the database

### FCM Token Missing
- If a user hasn't registered their FCM token, no notification is sent
- No error is thrown - this is expected behavior
- User can register token anytime via the endpoint

### Invalid Firebase Credentials
- App will fail to start if credentials file is invalid
- Check `FIREBASE_CREDENTIALS_PATH` is correct
- Ensure JSON file has proper permissions (readable)

## Monitoring & Logging

### Logs Generated
```
Firebase Messaging initialized successfully
FCM notification sent successfully (message_id=..., type=MONEY_RECEIVED)
Multicast FCM notification sent (success_count=2, failure_count=0)
Failed to send FCM notification (error=...)
```

### Check Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to **Cloud Messaging** → **Logs**
4. Filter by project ID to see delivery status

## Troubleshooting

### Notifications Not Being Sent
1. **Check Firebase is initialized:**
   - Look for "Firebase Messaging initialized successfully" in logs
   - Verify `FIREBASE_CREDENTIALS_PATH` is set

2. **Verify FCM token is registered:**
   - User must call `/api/v1/users/fcm-token` endpoint first
   - Check database: `SELECT fcm_token FROM users WHERE id = '<user_id>'`

3. **Check Firebase credentials:**
   - Verify JSON file exists at `FIREBASE_CREDENTIALS_PATH`
   - Confirm project ID matches `FIREBASE_PROJECT_ID`
   - Test credentials in Firebase Console

4. **Check Kotlin app setup:**
   - Firebase SDK properly integrated in app
   - Service account has "Cloud Messaging" enabled
   - AndroidManifest.xml has FCM service declared

### App Won't Start
- Check `.env` file for `FIREBASE_CREDENTIALS_PATH` and `FIREBASE_PROJECT_ID`
- Verify Firebase key JSON file path is absolute (not relative)
- Look for error messages in logs

## Security Notes

1. **Never commit Firebase key to Git:**
   - Add `firebase-key.json` to `.gitignore`
   - Store in environment variables or secure vault

2. **Restrict Firebase key permissions:**
   - The key should only have Cloud Messaging permissions
   - Don't use a key with full admin access

3. **Token rotation:**
   - FCM tokens can change; users may need to re-register
   - Consider periodic token refresh in the app

4. **Rate limiting:**
   - Notifications are sent asynchronously (non-blocking)
   - Each transfer creates 2 notifications (sender + receiver)

## Testing

### Manual Test

1. **Register FCM token:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/users/fcm-token \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"fcm_token": "test_token_xyz"}'
   ```

2. **Make a transfer:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/wallet/transfer \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "receiver_phone": "+1234567890",
       "amount_cents": 5000,
       "description": "Test transfer",
       "idempotency_key": "test-123456789"
     }'
   ```

3. **Check notifications:**
   ```bash
   curl http://localhost:8080/api/v1/users/notifications \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

## Next Steps

1. ✅ Backend: Firebase implementation complete
2. ⏳ Mobile (Kotlin): Integrate Firebase SDK and register FCM tokens
3. ⏳ Mobile (Kotlin): Handle incoming notifications in `FirebaseMessagingService`
4. ⏳ Optional: Add notification settings (mute, email notifications, etc.)
5. ⏳ Optional: Add notification center UI to show past notifications

## Files Modified/Created

```
backend/
├── migrations/
│   └── 202605220000_add_fcm_token_to_users.sql (NEW)
├── internal/
│   ├── config/
│   │   ├── config.go (UPDATED - added Firebase config fields)
│   │   └── firebase.go (NEW)
│   ├── models/
│   │   └── models.go (UPDATED - added Notification model & FCMToken field)
│   ├── dto/
│   │   └── dto.go (UPDATED - added DTOs for FCM and notifications)
│   ├── services/
│   │   ├── services.go (UPDATED - added notification methods)
│   │   └── notification_service.go (NEW)
│   ├── repositories/
│   │   └── repositories.go (UPDATED - added notification repo methods)
│   ├── handlers/
│   │   └── handlers.go (UPDATED - added FCM & notification handlers)
│   └── routes/
│       └── routes.go (UPDATED - added new endpoints)
├── cmd/api/
│   └── main.go (UPDATED - Firebase initialization)
├── go.mod (UPDATED - added firebase.google.com/go/v4)
├── .env.example (UPDATED - added Firebase vars)
└── README.md (this file)
```

## Version Info

- Firebase Admin SDK v4.14.0
- Go 1.26.1
- Gin v1.12.0
- GORM v1.31.1

---

**Questions?** Check the inline code comments or review the notification flow in `services.go` → `sendTransferNotifications()`.
