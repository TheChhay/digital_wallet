package com.app.digitalwallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.data.Transaction
import com.app.digitalwallet.data.WalletInfo
import com.app.digitalwallet.data.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class WalletViewModel(private val repository: WalletRepository = WalletRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadWalletData()
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
            repository.getAllTransactions()
                .catch { _transactions.value = emptyList() }
                .collect { list ->
                    _transactions.value = list
                }
        }
    }

    fun sendMoney(phone: String, amount: Double, note: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.sendMoney(phone, amount, note)
            if (success) refresh()
            onComplete(success)
        }
    }

    fun deposit(amount: Double, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deposit(amount)
            if (success) refresh()
            onComplete(success)
        }
    }

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
