package com.app.digitalwallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.api.dto.ValidateTokenResponse
import com.app.digitalwallet.data.QRRepository
import com.app.digitalwallet.data.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QRUiState {
    object Idle : QRUiState()
    object Loading : QRUiState()
    data class Success<T>(val data: T) : QRUiState()
    data class Error(val message: String) : QRUiState()
}

@HiltViewModel
class QRViewModel @Inject constructor(
    private val repository: QRRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QRUiState>(QRUiState.Idle)
    val uiState: StateFlow<QRUiState> = _uiState.asStateFlow()

    private val _timerProgress = MutableStateFlow(1f)
    val timerProgress: StateFlow<Float> = _timerProgress.asStateFlow()

    // Holds the validated payment data between scanner → pay screen
    private val _pendingPayment = MutableStateFlow<ValidateTokenResponse?>(null)
    val pendingPayment: StateFlow<ValidateTokenResponse?> = _pendingPayment.asStateFlow()

    private var qrJob: Job? = null
    private var timerJob: Job? = null
    private var isGenerationActive = false

    fun generateDynamicQR(walletId: String, amount: Double, currency: String) {
        isGenerationActive = true
        startGenerationLoop(walletId, amount, currency)
    }

    private fun startGenerationLoop(walletId: String, amount: Double, currency: String) {
        qrJob?.cancel()
        timerJob?.cancel()

        qrJob = viewModelScope.launch {
            _uiState.value = QRUiState.Loading
            try {
                val response = repository.generateDynamicQR(walletId, amount, currency)
                if (response != null && isGenerationActive) {
                    _uiState.value = QRUiState.Success(response)
                    startTimer(walletId, amount, currency)
                } else if (isGenerationActive) {
                    _uiState.value = QRUiState.Error("Failed to generate QR")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (isGenerationActive) {
                    _uiState.value = QRUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun startTimer(walletId: String, amount: Double, currency: String) {
        timerJob = viewModelScope.launch {
            val duration = 60000L
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < duration) {
                if (!isGenerationActive) return@launch
                val elapsed = System.currentTimeMillis() - startTime
                _timerProgress.value = 1f - (elapsed.toFloat() / duration)
                delay(100)
            }
            if (isGenerationActive) {
                startGenerationLoop(walletId, amount, currency)
            }
        }
    }

    fun getStaticQR(address: String) {
        viewModelScope.launch {
            _uiState.value = QRUiState.Loading
            try {
                val response = repository.getStaticQR(address)
                if (response != null) {
                    _uiState.value = QRUiState.Success(response)
                } else {
                    _uiState.value = QRUiState.Error("Failed to load static QR")
                }
            } catch (e: Exception) {
                _uiState.value = QRUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun validateScannedTokenAsync(token: String): ValidateTokenResponse? {
        return try {
            val response = repository.validateToken(token)
            if (response != null && response.isValid) {
                _uiState.value = QRUiState.Success(response)
                _pendingPayment.value = response
                response
            } else {
                _uiState.value = QRUiState.Error(response?.message ?: "Invalid token")
                null
            }
        } catch (e: Exception) {
            _uiState.value = QRUiState.Error(e.message ?: "Validation error")
            null
        }
    }

    suspend fun resolveRecipient(phone: String? = null, walletId: String? = null, walletViewModel: WalletViewModel): WalletRepository.RecipientLookupResult? {
        return kotlin.coroutines.suspendCoroutine { continuation ->
            walletViewModel.lookupRecipient(phone = phone, walletId = walletId) { result ->
                continuation.resumeWith(Result.success(result))
            }
        }
    }

    // Called by scanner after successful validation or for static QR
    fun setPendingPayment(response: ValidateTokenResponse) {
        _pendingPayment.value = response
    }


    // Called by pay screen after payment is done or user cancels
    fun clearPendingPayment() {
        _pendingPayment.value = null
    }

    fun stopQRGeneration() {
        isGenerationActive = false
        qrJob?.cancel()
        timerJob?.cancel()
        _uiState.value = QRUiState.Idle
        _timerProgress.value = 1f
    }
}