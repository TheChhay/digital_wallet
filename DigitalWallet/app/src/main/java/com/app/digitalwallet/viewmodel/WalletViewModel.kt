package com.app.digitalwallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.data.Transaction
import com.app.digitalwallet.data.WalletInfo
import com.app.digitalwallet.data.WalletRepository
import com.app.digitalwallet.util.NotificationHelper
import com.app.digitalwallet.utils.PhoneNumberUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: WalletRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

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
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _transferStatus = MutableStateFlow<TransferStatus>(TransferStatus.Idle)
    val transferStatus: StateFlow<TransferStatus> = _transferStatus.asStateFlow()

    init {
        refresh()
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

    private fun loadWalletData() {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading
            repository.getWalletInfo()
                .catch { e ->
                    _uiState.value = WalletUiState.Error(e.message ?: "Failed to load wallet info")
                }
                .collect { walletInfo ->
                    _uiState.value = WalletUiState.Success(walletInfo)
                }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            repository.getAllTransactions(null)
                .catch { _transactions.value = emptyList() }
                .collect { page ->
                    _transactions.value = page.transactions
                    _nextCursor.value = page.nextCursor
                    _hasMore.value = page.hasMore
                }
        }
    }

//    fun loadMoreTransactions() {
//        if (_isLoadingMore.value || !_hasMore.value) return
//
//        viewModelScope.launch {
//            _isLoadingMore.value = true
//            repository.getAllTransactions(_nextCursor.value)
//                .catch { _isLoadingMore.value = false }
//                .collect { page ->
//                    _transactions.value = _transactions.value + page.transactions
//                    _nextCursor.value = page.nextCursor
//                    _hasMore.value = page.hasMore
//                    _isLoadingMore.value = false
//                }
//        }
//    }

    fun transferMoney(phone: String? = null, walletId: String? = null, amount: Double, note: String?, onComplete: (Boolean) -> Unit) {
        val currentBalance = (uiState.value as? WalletUiState.Success)?.walletInfo?.balance ?: 0.0
        if (amount > currentBalance) {
            _transferStatus.value = TransferStatus.Error("Insufficient balance")
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _transferStatus.value = TransferStatus.Loading
            try {
                val finalRecipientPhone = phone?.let { PhoneNumberUtils.normalize(it) }
                val success = repository.transferMoney(
                    recipientPhone = finalRecipientPhone,
                    walletId = walletId,
                    amount = amount,
                    note = note
                )
                if (success) {
                    refresh()
                    _transferStatus.value = TransferStatus.Success
                    notificationHelper.showNotification(
                        title = "Transfer Successful",
                        message = "You have successfully transferred $${String.format(Locale.US, "%.2f", amount)}."
                    )
                } else {
                    _transferStatus.value = TransferStatus.Error("Transfer failed. Please try again.")
                }
                onComplete(success)
            } catch (e: Exception) {
                _transferStatus.value = TransferStatus.Error(e.message ?: "An unexpected error occurred")
                onComplete(false)
            }
        }
    }

    fun lookupRecipient(phone: String? = null, walletId: String? = null, onResult: (WalletRepository.RecipientLookupResult?) -> Unit) {
        viewModelScope.launch {
            val normalizedPhone = phone?.let { PhoneNumberUtils.normalize(it) }
            val result = repository.lookupRecipient(
                phone = normalizedPhone,
                walletId = walletId
            )
            onResult(result)
        }
    }


    //skip for MVP
    fun deposit(amount: Double, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deposit(amount)
            if (success) refresh()
            onComplete(success)
        }
    }

    //skip for MVP
    fun withdraw(amount: Double, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.withdraw(amount)
            if (success) refresh()
            onComplete(success)
        }
    }
}

sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Success(val walletInfo: WalletInfo) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}

sealed class TransferStatus {
    object Idle : TransferStatus()
    object Loading : TransferStatus()
    object Success : TransferStatus()
    data class Error(val message: String) : TransferStatus()
}
