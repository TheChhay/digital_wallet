package com.app.digitalwallet.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.api.dto.LoginRequest
import com.app.digitalwallet.api.dto.RegisterRequest
import com.app.digitalwallet.auth.SessionManager
import com.app.digitalwallet.data.AuthRepository
import com.app.digitalwallet.utils.PhoneNumberUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val logoutEvent = sessionManager.logoutEvent

    var loginUiState by mutableStateOf(AuthUiState())
        private set

    var registerUiState by mutableStateOf(AuthUiState())
        private set

    fun login(phone: String, password: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            loginUiState = loginUiState.copy(isLoading = true, error = null)
            val normalizedPhone = PhoneNumberUtils.normalize(phone)
            try {
                val response = repository.login(LoginRequest(normalizedPhone, password))
                if (response.success && response.data != null) {
                    loginUiState = loginUiState.copy(isLoading = false)
                    onLoginSuccess()
                } else {
                    loginUiState = loginUiState.copy(isLoading = false, error = response.message)
                }
            } catch (e: Exception) {
                loginUiState = loginUiState.copy(isLoading = false, error = e.localizedMessage ?: "An error occurred")
            }
        }
    }

    fun register(phone: String, fullName: String, password: String, onRegisterSuccess: () -> Unit) {
        viewModelScope.launch {
            registerUiState = registerUiState.copy(isLoading = true, error = null)
            val normalizedPhone = PhoneNumberUtils.normalize(phone)
            try {
                val response = repository.register(RegisterRequest(normalizedPhone, password, fullName))
                if (response.success) {
                    registerUiState = registerUiState.copy(isLoading = false)
                    onRegisterSuccess()
                } else {
                    registerUiState = registerUiState.copy(isLoading = false, error = response.message)
                }
            } catch (e: Exception) {
                registerUiState = registerUiState.copy(isLoading = false, error = e.localizedMessage ?: "An error occurred")
            }
        }
    }

    fun clearLoginError() {
        loginUiState = loginUiState.copy(error = null)
    }

    fun clearRegisterError() {
        registerUiState = registerUiState.copy(error = null)
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
