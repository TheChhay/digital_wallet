package com.app.digitalwallet.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.domain.usecase.auth.*
import com.app.digitalwallet.core.session.SessionManager
import com.app.digitalwallet.domain.model.User
import com.app.digitalwallet.utils.PhoneNumberUtils
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

sealed class AuthUiEvent {
    object NavigateToHome : AuthUiEvent()
    object NavigateToRegister : AuthUiEvent()
    object ProfileUpdated : AuthUiEvent()
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class RegisterUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isUploadingImage: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val getMeUseCase: GetMeUseCase,
    private val updateProfileImageUseCase: UpdateProfileImageUseCase,
    private val updateFcmTokenUseCase: UpdateFcmTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<AuthUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    val userProfile get() = _profileUiState.value.user

    val logoutEvent = sessionManager.logoutEvent

    fun getMe() {
        viewModelScope.launch {
            _profileUiState.update { it.copy(isLoading = true, error = null) }
            val result = getMeUseCase()
            result.onSuccess { user ->
                _profileUiState.update { it.copy(user = user, isLoading = false) }
                syncFcmToken()
            }.onFailure { error ->
                _profileUiState.update { it.copy(isLoading = false, error = error.localizedMessage) }
            }
        }
    }

    private fun syncFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                viewModelScope.launch {
                    updateFcmTokenUseCase(token)
                }
            }
        }
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _loginUiState.update { it.copy(isLoading = true, error = null) }
            val normalizedPhone = PhoneNumberUtils.normalize(phone)
            val result = loginUseCase(normalizedPhone, password)
            result.onSuccess {
                _loginUiState.update { it.copy(isLoading = false) }
                syncFcmToken()
                _uiEvent.emit(AuthUiEvent.NavigateToHome)
            }.onFailure { error ->
                _loginUiState.update {
                    it.copy(isLoading = false, error = error.localizedMessage ?: "An error occurred")
                }
            }
        }
    }

    fun register(phone: String, firstName: String, lastName: String, password: String) {
        viewModelScope.launch {
            _registerUiState.update { it.copy(isLoading = true, error = null) }
            val normalizedPhone = PhoneNumberUtils.normalize(phone)
            val result = registerUseCase(normalizedPhone, password, firstName, lastName)
            result.onSuccess {
                _registerUiState.update { it.copy(isLoading = false) }
                _uiEvent.emit(AuthUiEvent.NavigateToHome)
            }.onFailure { error ->
                _registerUiState.update {
                    it.copy(isLoading = false, error = error.localizedMessage ?: "An error occurred")
                }
            }
        }
    }

    fun updateProfileImage(image: MultipartBody.Part) {
        viewModelScope.launch {
            _profileUiState.update { it.copy(isUploadingImage = true, error = null) }
            val result = updateProfileImageUseCase(image)
            result.onSuccess { user ->
                _profileUiState.update { it.copy(user = user, isUploadingImage = false) }
                _uiEvent.emit(AuthUiEvent.ProfileUpdated)
            }.onFailure { error ->
                _profileUiState.update {
                    it.copy(isUploadingImage = false, error = error.localizedMessage ?: "An error occurred")
                }
            }
        }
    }

    fun clearLoginError() {
        _loginUiState.update { it.copy(error = null) }
    }

    fun clearRegisterError() {
        _registerUiState.update { it.copy(error = null) }
    }

    fun logout() {
        logoutUseCase()
        sessionManager.triggerLogout()
    }
}
