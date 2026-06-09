package com.app.digitalwallet.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.domain.model.Transaction
import com.app.digitalwallet.domain.model.Wallet
import com.app.digitalwallet.domain.repository.RecipientLookupResult
import com.app.digitalwallet.domain.usecase.wallet.*
import com.app.digitalwallet.utils.RefreshEventBus
import com.app.digitalwallet.utils.NotificationHelper
import com.app.digitalwallet.utils.PhoneNumberUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class WalletUiEvent {
    object TransferSuccess : WalletUiEvent()
    data class ShowError(val message: String) : WalletUiEvent()
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getWalletInfoUseCase: GetWalletInfoUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val transferMoneyUseCase: TransferMoneyUseCase,
    private val lookupRecipientUseCase: LookupRecipientUseCase,
    private val notificationHelper: NotificationHelper,
    private val refreshEventBus: RefreshEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<WalletUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    val todayTransactions: StateFlow<List<Transaction>> = _transactions.map { list ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())
        list.filter { it.date == today || it.date == "Today" }
            .sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _nextCursor = MutableStateFlow<String?>(null)
    private val _hasMore = MutableStateFlow(true)

    private val _transferStatus = MutableStateFlow<TransferStatus>(TransferStatus.Idle)
    val transferStatus: StateFlow<TransferStatus> = _transferStatus.asStateFlow()

    init {
        refresh()
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshEventBus.refreshEvents.collect { type ->
                when (type) {
                    RefreshEventBus.RefreshType.WALLET -> loadWalletData()
                    RefreshEventBus.RefreshType.TRANSACTIONS -> loadTransactions()
                    RefreshEventBus.RefreshType.ALL -> refresh()
                }
            }
        }
    }

    fun resetTransferStatus() {
        _transferStatus.value = TransferStatus.Idle
    }

    fun refresh() {
        loadWalletData()
        _nextCursor.value = null
        _hasMore.value = true
        loadTransactions()
    }

    private var loadWalletJob: kotlinx.coroutines.Job? = null

    private fun loadWalletData() {
        loadWalletJob?.cancel()
        loadWalletJob = viewModelScope.launch {
            _uiState.value = WalletUiState.Loading
            try {
                val wallet = getWalletInfoUseCase()
                _uiState.value = WalletUiState.Success(wallet)
            } catch (e: Exception) {
                _uiState.value = WalletUiState.Error(e.message ?: "Failed to load wallet info")
            }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            getTransactionsUseCase(null)
                .catch { _transactions.value = emptyList() }
                .collect { page ->
                    _transactions.value = page.transactions
                    _nextCursor.value = page.nextCursor
                    _hasMore.value = page.hasMore
                }
        }
    }

    fun transferMoney(phone: String? = null, walletId: String? = null, amount: Double, note: String?) {
        viewModelScope.launch {
            _transferStatus.value = TransferStatus.Loading
            try {
                val finalRecipientPhone = phone?.let { PhoneNumberUtils.normalize(it) }
                val result = transferMoneyUseCase(
                    phone = finalRecipientPhone,
                    walletId = walletId,
                    amount = amount,
                    note = note
                )
                
                result.onSuccess {
                    refresh()
                    _transferStatus.value = TransferStatus.Success
                    _uiEvent.emit(WalletUiEvent.TransferSuccess)
                    notificationHelper.showNotification(
                        title = "Transfer Successful",
                        message = "You have successfully transferred $${String.format(Locale.US, "%.2f", amount)}."
                    )
                }.onFailure { e ->
                    _transferStatus.value = TransferStatus.Error(e.message ?: "Transfer failed. Please try again.")
                    _uiEvent.emit(WalletUiEvent.ShowError(e.message ?: "Transfer failed"))
                }
            } catch (e: Exception) {
                _transferStatus.value = TransferStatus.Error(e.message ?: "An unexpected error occurred")
                _uiEvent.emit(WalletUiEvent.ShowError(e.message ?: "An unexpected error occurred"))
            }
        }
    }

    fun lookupRecipient(phone: String? = null, walletId: String? = null, onResult: (RecipientLookupResult?) -> Unit) {
        viewModelScope.launch {
            val normalizedPhone = phone?.let { PhoneNumberUtils.normalize(it) }
            val result = lookupRecipientUseCase(
                phone = normalizedPhone,
                walletId = walletId
            )
            onResult(result)
        }
    }
}

sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Success(val wallet: Wallet) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}

sealed class TransferStatus {
    object Idle : TransferStatus()
    object Loading : TransferStatus()
    object Success : TransferStatus()
    data class Error(val message: String) : TransferStatus()
}
