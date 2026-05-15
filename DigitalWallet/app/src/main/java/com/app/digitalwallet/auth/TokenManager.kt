package com.app.digitalwallet.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class TokenManager private constructor(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun saveTokens(accessToken: String, refreshToken: String, phone: String? = null) {
        sharedPreferences.edit {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            phone?.let { putString("user_phone", it) }
        }
    }

    fun getAccessToken(): String? = sharedPreferences.getString("access_token", null)
    
    fun getRefreshToken(): String? = sharedPreferences.getString("refresh_token", null)

    fun getUserPhone(): String? = sharedPreferences.getString("user_phone", null)

    fun clearTokens() {
        sharedPreferences.edit { clear() }
    }
}
