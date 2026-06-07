package com.app.digitalwallet.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshEventBus @Inject constructor() {
    private val _refreshEvents = MutableSharedFlow<RefreshType>(extraBufferCapacity = 1)
    val refreshEvents = _refreshEvents.asSharedFlow()

    suspend fun emitRefresh(type: RefreshType) {
        _refreshEvents.emit(type)
    }

    enum class RefreshType {
        WALLET,
        TRANSACTIONS,
        ALL
    }
}
