package com.app.digitalwallet.core.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.app.digitalwallet.MainActivity
import com.app.digitalwallet.R
import com.app.digitalwallet.domain.repository.IAuthRepository
import com.app.digitalwallet.utils.RefreshEventBus
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WalletNotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: IAuthRepository

    @Inject
    lateinit var refreshEventBus: RefreshEventBus

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "WalletFCM"
        private const val CHANNEL_ID = "wallet_notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        
        // Update the token in your backend via AuthRepository
        serviceScope.launch {
            authRepository.updateFcmToken(token).onFailure { e ->
                Log.e(TAG, "Failed to update FCM token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. You can fetch full notification details from the API
        // This ensures you have the latest data (e.g. amount, transaction ID)
        serviceScope.launch {
            val result = authRepository.getNotification()
            result.onSuccess { notification ->
                handleNotification(
                    type = notification.type,
                    title = notification.title,
                    message = notification.message
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to fetch notification details", e)
            }
        }

        // 2. Fallback or standard handling for data payload
        if (remoteMessage.data.isNotEmpty() && remoteMessage.data["type"] != "sync") {
            val type = remoteMessage.data["type"]
            val title = remoteMessage.data["title"] ?: getString(R.string.app_name)
            val message = remoteMessage.data["message"] ?: ""
            handleNotification(type, title, message)
        }

        // Handle notification payload (if any)
        remoteMessage.notification?.let {
            showNotification(it.title ?: getString(R.string.app_name), it.body ?: "")
        }
    }

    private fun handleNotification(type: String?, title: String, message: String) {
        // Trigger UI refresh when a notification is received
        serviceScope.launch {
            refreshEventBus.emitRefresh(RefreshEventBus.RefreshType.ALL)
        }

        // Specific logic based on wallet notification types
        when (type) {
            "transaction_alert", "low_balance" -> {
                showNotification(title, message)
            }
            "fraud_alert" -> {
                showNotification(title, message, true)
            }
            else -> {
                showNotification(title, message)
            }
        }
    }

    private fun showNotification(title: String, message: String, isHighPriority: Boolean = false) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (isHighPriority) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }
            val channel = NotificationChannel(
                CHANNEL_ID, 
                getString(R.string.notification_channel_name), 
                importance
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(if (isHighPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
