package com.app.digitalwallet.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.api.dto.LoginRequest
import com.app.digitalwallet.api.dto.RegisterRequest
import com.app.digitalwallet.api.dto.UserProfileRequest
import com.app.digitalwallet.api.dto.UserResponse
import com.app.digitalwallet.auth.SessionManager
import com.app.digitalwallet.data.AuthRepository
import com.app.digitalwallet.utils.PhoneNumberUtils
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

sealed class AuthUiEffect {
    object NavigateToHome : AuthUiEffect()
    object NavigateToRegister : AuthUiEffect()
    object ProfileUpdated : AuthUiEffect()
    data class ShowError(val message: String) : AuthUiEffect()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiEffect = MutableSharedFlow<AuthUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    val logoutEvent = sessionManager.logoutEvent

    var loginUiState by mutableStateOf(AuthUiState())
        private set

    var registerUiState by mutableStateOf(AuthUiState())
        private set

    var uploadProfileImageUiState by mutableStateOf(AuthUiState())
        private set

    var updateProfileUiState by mutableStateOf(AuthUiState())  // ✅ fixed name + private set
        private set

    var userProfile by mutableStateOf<UserResponse?>(null)
        private set

    fun getMe() {
        viewModelScope.launch {
            try {
                val response = repository.getMe()
                if (response.success) {
                    userProfile = response.data
                    // Update FCM token when we know the user is authenticated
                    syncFcmToken()
                } else {
                    // optional: log or surface error if needed
                }
            } catch (e: Exception) {
                // getMe is usually a background refresh, so silent is acceptable
                // but at minimum log it: Log.e("AuthVM", "getMe failed", e)
            }
        }
    }

    private fun syncFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                viewModelScope.launch {
                    try {
                        repository.updateFcmToken(token)
                    } catch (e: Exception) {
                        // Silent failure for background sync
                    }
                }
            }
        }
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            loginUiState = loginUiState.copy(isLoading = true, error = null)
            val normalizedPhone = PhoneNumberUtils.normalize(phone)
            try {
                val response = repository.login(LoginRequest(normalizedPhone, password))
                if (response.success && response.data != null) {
                    loginUiState = loginUiState.copy(isLoading = false)
                    syncFcmToken() // Sync token after successful login
                    _uiEffect.emit(AuthUiEffect.NavigateToHome)
                } else {
                    loginUiState = loginUiState.copy(isLoading = false, error = response.message)
                }
            } catch (e: Exception) {
                loginUiState = loginUiState.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "An error occurred"
                )
            }
        }
    }

    fun register(phone: String, firstName: String, lastName: String, password: String) {
        viewModelScope.launch {
            registerUiState = registerUiState.copy(isLoading = true, error = null)
            val normalizedPhone = PhoneNumberUtils.normalize(phone)
            try {
                val response = repository.register(RegisterRequest(normalizedPhone, password, firstName, lastName))
                if (response.success) {
                    registerUiState = registerUiState.copy(isLoading = false)
                    _uiEffect.emit(AuthUiEffect.NavigateToHome) // Or wherever you want after register
                } else {
                    registerUiState = registerUiState.copy(isLoading = false, error = response.message)
                }
            } catch (e: Exception) {
                registerUiState = registerUiState.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "An error occurred"
                )
            }
        }
    }

    fun updateProfileImage(image: MultipartBody.Part) {
        viewModelScope.launch {
            uploadProfileImageUiState = uploadProfileImageUiState.copy(isLoading = true, error = null)
            try {
                val response = repository.updateProfileImage(image)
                if (response.success && response.data != null) {
                    userProfile = response.data
                    uploadProfileImageUiState = uploadProfileImageUiState.copy(isLoading = false)
                    _uiEffect.emit(AuthUiEffect.ProfileUpdated)
                } else {
                    uploadProfileImageUiState = uploadProfileImageUiState.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            } catch (e: Exception) {
                uploadProfileImageUiState = uploadProfileImageUiState.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "An error occurred"
                )
            }
        }
    }

    fun updateProfile(request: UserProfileRequest) {
        viewModelScope.launch {
            updateProfileUiState = updateProfileUiState.copy(isLoading = true, error = null)
            try {
                val response = repository.updateProfile(request)
                if (response.success && response.data != null) {
                    userProfile = response.data          // ✅ already UserResponse?, no cast needed
                    updateProfileUiState = updateProfileUiState.copy(isLoading = false)
                    _uiEffect.emit(AuthUiEffect.ProfileUpdated)
                } else {
                    updateProfileUiState = updateProfileUiState.copy(
                        isLoading = false,
                        error = response.message ?: "Update failed"  // ✅ user sees the error
                    )
                }
            } catch (e: Exception) {
                updateProfileUiState = updateProfileUiState.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "An error occurred"  // ✅ no silent failure
                )
            }
        }
    }

    fun clearLoginError() {
        loginUiState = loginUiState.copy(error = null)
    }

    fun clearRegisterError() {
        registerUiState = registerUiState.copy(error = null)
    }

    fun clearUpdateProfileError() {
        updateProfileUiState = updateProfileUiState.copy(error = null)
    }

    fun logout() {
        repository.logout()
        sessionManager.triggerLogout()
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
