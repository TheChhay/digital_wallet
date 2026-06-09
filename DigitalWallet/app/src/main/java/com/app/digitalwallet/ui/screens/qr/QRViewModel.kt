package com.app.digitalwallet.ui.screens.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.domain.model.ValidateQRData
import com.app.digitalwallet.domain.repository.IQRRepository
import com.app.digitalwallet.domain.repository.RecipientLookupResult
import com.app.digitalwallet.ui.screens.wallet.WalletViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QRUiState {
    object Idle : QRUiState()
    object Loading : QRUiState()
    data class Success<T>(val data: T) : QRUiState()
    data class Error(val message: String) : QRUiState()
}

sealed class QRUiEvent {
    object ValidationSuccess : QRUiEvent()
    data class ShowError(val message: String) : QRUiEvent()
}

@HiltViewModel
class QRViewModel @Inject constructor(
    private val repository: IQRRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QRUiState>(QRUiState.Idle)
    val uiState: StateFlow<QRUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<QRUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _timerProgress = MutableStateFlow(1f)
    val timerProgress: StateFlow<Float> = _timerProgress.asStateFlow()

    // Holds the validated payment data between scanner → pay screen
    private val _pendingPayment = MutableStateFlow<ValidateQRData?>(null)
    val pendingPayment: StateFlow<ValidateQRData?> = _pendingPayment.asStateFlow()

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
            repository.generateDynamicQR(walletId, amount, currency)
                .onSuccess { data ->
                    if (isGenerationActive) {
                        _uiState.value = QRUiState.Success(data)
                        startTimer(walletId, amount, currency)
                    }
                }
                .onFailure { error ->
                    if (isGenerationActive) {
                        _uiState.value = QRUiState.Error(error.message ?: "Failed to generate QR")
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
            repository.getStaticQR(address)
                .onSuccess { data ->
                    _uiState.value = QRUiState.Success(data)
                }
                .onFailure { error ->
                    _uiState.value = QRUiState.Error(error.message ?: "Failed to load static QR")
                }
        }
    }

    suspend fun validateScannedTokenAsync(token: String): ValidateQRData? {
        val result = repository.validateToken(token)
        return result.fold(
            onSuccess = { data ->
                if (data.isValid) {
                    _uiState.value = QRUiState.Success(data)
                    _pendingPayment.value = data
                    _uiEvent.emit(QRUiEvent.ValidationSuccess)
                    data
                } else {
                    _uiState.value = QRUiState.Error(data.message ?: "Invalid token")
                    null
                }
            },
            onFailure = { error ->
                _uiState.value = QRUiState.Error(error.message ?: "Validation error")
                null
            }
        )
    }

    suspend fun resolveRecipient(phone: String? = null, walletId: String? = null, walletViewModel: WalletViewModel): RecipientLookupResult? {
        return kotlin.coroutines.suspendCoroutine { continuation ->
            walletViewModel.lookupRecipient(phone = phone, walletId = walletId) { result ->
                continuation.resumeWith(Result.success(result))
            }
        }
    }

    // Called by scanner after successful validation or for static QR
    fun setPendingPayment(data: ValidateQRData) {
        _pendingPayment.value = data
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
