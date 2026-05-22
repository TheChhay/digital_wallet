# Firebase Notifications - Kotlin Integration Guide

## Quick Start for Your Kotlin App

### Step 1: Add Firebase Dependencies (build.gradle)

```gradle
dependencies {
    // Firebase Messaging
    implementation 'com.google.firebase:firebase-messaging:23.4.1'
    
    // Retrofit (for API calls)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
}
```

### Step 2: Get FCM Token on App Launch

```kotlin
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get FCM token and send to backend
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Token: $token")
                
                // Send to backend
                registerFCMToken(token)
            } else {
                Log.e("FCM", "Failed to get token", task.exception)
            }
        }
    }
    
    private fun registerFCMToken(token: String) {
        val authToken = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("access_token", "") ?: return
        
        // Create API request
        val request = RegisterFCMTokenRequest(fcm_token = token)
        
        // Send to backend
        apiService.registerFCMToken(
            "Bearer $authToken",
            request
        ).enqueue(object : Callback<ApiResponse<Unit>> {
            override fun onResponse(
                call: Call<ApiResponse<Unit>>,
                response: Response<ApiResponse<Unit>>
            ) {
                if (response.isSuccessful) {
                    Log.d("FCM", "Token registered successfully")
                } else {
                    Log.e("FCM", "Failed to register token: ${response.code()}")
                }
            }
            
            override fun onFailure(call: Call<ApiResponse<Unit>>, t: Throwable) {
                Log.e("FCM", "Error registering token", t)
            }
        })
    }
}
```

### Step 3: Create FCM Message Handler

```kotlin
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.util.Log

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received from: ${remoteMessage.from}")
        
        // Extract notification data
        val title = remoteMessage.notification?.title ?: "Digital Wallet"
        val body = remoteMessage.notification?.body ?: ""
        val type = remoteMessage.data["type"] ?: "UNKNOWN"
        val amount = remoteMessage.data["amount"] ?: ""
        
        Log.d("FCM", "Title: $title, Body: $body, Type: $type")
        
        // Show notification
        showNotification(title, body, type, amount)
    }
    
    override fun onNewToken(token: String) {
        Log.d("FCM", "New token: $token")
        
        // Save token to SharedPreferences
        val sharedPref = getSharedPreferences("fcm", MODE_PRIVATE)
        sharedPref.edit().putString("token", token).apply()
        
        // Register new token with backend
        registerNewToken(token)
    }
    
    private fun showNotification(
        title: String,
        body: String,
        type: String,
        amount: String
    ) {
        val channelId = "wallet_notifications"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel (required for API 26+)
        val channel = NotificationChannel(
            channelId,
            "Wallet Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for money transfers"
        }
        notificationManager.createNotificationChannel(channel)
        
        // Create intent to open app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("notification_type", type)
            putExtra("amount", amount)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification) // Use your app icon
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        // Show notification
        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
    
    private fun registerNewToken(token: String) {
        // Similar to registerFCMToken in MainActivity
        // Call your API service to register the new token
    }
}
```

### Step 4: Update AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Permissions for notifications -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <application>
        <!-- Your activities -->
        
        <!-- Firebase Messaging Service -->
        <service
            android:name=".MyFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
        
    </application>
</manifest>
```

### Step 5: Request Notification Permission (Android 13+)

```kotlin
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.Manifest

class MainActivity : AppCompatActivity() {
    private val NOTIFICATION_PERMISSION_CODE = 101
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
        
        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                registerFCMToken(task.result)
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)
            ) {
                // Permission granted
                Log.d("FCM", "Notification permission granted")
            }
        }
    }
}
```

### Step 6: Create API Service

```kotlin
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class RegisterFCMTokenRequest(
    val fcm_token: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: Any? = null
)

interface WalletApiService {
    @POST("/api/v1/users/fcm-token")
    fun registerFCMToken(
        @Header("Authorization") authToken: String,
        @Body request: RegisterFCMTokenRequest
    ): Call<ApiResponse<Unit>>
    
    @GET("/api/v1/users/notifications")
    fun getNotifications(
        @Header("Authorization") authToken: String
    ): Call<ApiResponse<List<NotificationResponse>>>
}

data class NotificationResponse(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val amount: Double?,
    val related_tx_id: String?,
    val is_read: Boolean,
    val created_at: String
)
```

### Step 7: Handle Notification Taps

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if activity was launched from notification
        val notificationType = intent.getStringExtra("notification_type")
        val amount = intent.getStringExtra("amount")
        
        if (notificationType != null) {
            Log.d("Notification", "App opened from notification: $notificationType")
            // Handle notification tap - e.g., navigate to transactions page
            when (notificationType) {
                "MONEY_RECEIVED", "MONEY_SENT" -> {
                    // Navigate to transactions page
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, TransactionsFragment())
                        .commit()
                }
            }
        }
    }
}
```

## Testing Notifications

### Test 1: Send Test Notification from Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Go to **Cloud Messaging** → **Send your first message**
4. Enter title and body
5. Select target audience (your app)
6. Send

### Test 2: Make Transfer and Check Notification
1. Log in as User A in the app
2. Register FCM token (automatic on app launch)
3. Make a transfer to User B
4. Check notifications on both accounts
5. Verify notifications appear in the app

### Test 3: Check API Response
```bash
# Get notifications for current user
curl https://your-api.com/api/v1/users/notifications \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Expected Notification Formats

### Money Received
```
Title: "Money Received"
Body: "You received $50.00 from John Doe"
Data: {
  "type": "MONEY_RECEIVED",
  "amount": "50.00",
  "sender_name": "John Doe"
}
```

### Money Sent
```
Title: "Transfer Successful"
Body: "You sent $50.00 to Jane Smith"
Data: {
  "type": "MONEY_SENT",
  "amount": "50.00",
  "receiver_name": "Jane Smith"
}
```

## Troubleshooting

### Notifications Not Showing
1. **Check permissions:** Settings → Apps → Your App → Permissions → Notifications (ON)
2. **Check Firebase token:** Log should show "Token registered successfully"
3. **Check backend:** Verify transfer was successful (Status: 200)
4. **Check Android version:** Notifications work on all versions, but permission needed on 13+

### Firebase Token Not Registering
1. Check network connection
2. Verify auth token is valid
3. Check API endpoint is correct
4. Look for errors in Logcat

### Notification Service Not Called
1. Add `Log.d()` statements to verify service is running
2. Check AndroidManifest.xml has service declaration
3. Verify Firebase dependencies are correctly added
4. Check that service name matches exactly

## Important Notes for Your App

- **Register token on app launch** - It may change periodically
- **Re-register on token refresh** - Implement `onNewToken()` callback
- **Handle notification taps** - Use PendingIntent to navigate to relevant screen
- **Store auth token securely** - Don't put access token in logs
- **Test on real device** - Emulator may not receive notifications
- **Check battery optimization** - Some devices may block background services

## Timeline for Integration

1. ✅ Backend: Firebase implementation complete
2. ⏳ Your task: Add Firebase SDK to Kotlin app
3. ⏳ Your task: Create FCM service handler
4. ⏳ Your task: Request notification permission
5. ⏳ Your task: Register FCM token with backend
6. ⏳ Your task: Handle notification taps
7. ✅ Both ready for testing!

---

**Need help?** Check the backend logs for Firebase errors, or verify your Firebase project is configured correctly in the Firebase Console.
