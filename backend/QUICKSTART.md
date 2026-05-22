# Firebase Notifications - Quick Start (5 Minutes)

## What's Ready

✅ Backend has complete Firebase integration
✅ Code builds successfully  
✅ Ready to deploy and configure

## Get Started Now

### Step 1: Get Firebase Key (2 min)
1. Open [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to **Project Settings** (⚙️ icon, top-right)
4. Click **Service Accounts** tab
5. Click **Generate New Private Key** → Save the JSON file

### Step 2: Configure Backend (1 min)
```bash
# Edit your .env file and add:
FIREBASE_CREDENTIALS_PATH=/full/path/to/firebase-key.json
FIREBASE_PROJECT_ID=your-project-id
```

Get your project ID from the JSON file or Firebase Console (visible in URL).

### Step 3: Run Migration (1 min)
```bash
goose -dir backend/migrations postgres "$DATABASE_URL" up
```

### Step 4: Start Backend (1 min)
```bash
cd backend
go build -o api ./cmd/api/main.go
./api
```

You should see:
```
Firebase Messaging initialized successfully
api server starting addr=:8080
```

## Test It Works

### Option 1: Test with cURL
```bash
# 1. Register FCM token
curl -X POST http://localhost:8080/api/v1/users/fcm-token \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fcm_token": "test_token_xyz"}'

# 2. Make a transfer (from your app or API)

# 3. Check notifications
curl http://localhost:8080/api/v1/users/notifications \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Option 2: Test with Kotlin App
1. Open your Kotlin project
2. Add Firebase dependency (see KOTLIN_INTEGRATION.md)
3. App will auto-register FCM token on launch
4. Make transfer → Get notification!

## What Each File Does

| File | Purpose |
|------|---------|
| `internal/config/firebase.go` | Initializes Firebase SDK |
| `internal/services/notification_service.go` | Sends FCM messages |
| `internal/handlers/handlers.go` | API endpoints (register token, get notifications) |
| `migrations/202605220000_add_fcm_token_to_users.sql` | Database schema update |

## API Endpoints

```
POST /api/v1/users/fcm-token
  Register device token from Kotlin app

GET /api/v1/users/notifications  
  Get past notifications
```

## Next Steps

1. ✅ Configure Firebase credentials
2. ✅ Run database migration
3. ✅ Start backend
4. ⏳ Integrate Kotlin app (see KOTLIN_INTEGRATION.md)
5. ⏳ Test with real transfer

## Troubleshooting

**Backend won't start?**
- Check `FIREBASE_CREDENTIALS_PATH` exists
- Verify JSON file path is absolute (not relative)
- Check file permissions (readable)

**Notifications not sending?**
- Check logs show "Firebase Messaging initialized successfully"
- Verify FCM token registered: check database
- Try test message in Firebase Console

**Build fails?**
```bash
cd backend
go mod tidy
go build -o api ./cmd/api/main.go
```

## Files to Read

- `FIREBASE_SETUP.md` - Complete documentation
- `KOTLIN_INTEGRATION.md` - Kotlin app setup
- `IMPLEMENTATION_SUMMARY.md` - What was built

## That's It! 🎉

Your backend is ready. Now integrate your Kotlin app and you're done!

For detailed setup, see **FIREBASE_SETUP.md** and **KOTLIN_INTEGRATION.md**.

---

**Questions?** Check the full documentation files or review inline code comments.
