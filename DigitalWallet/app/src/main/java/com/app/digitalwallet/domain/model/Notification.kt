package com.app.digitalwallet.domain.model

data class Notification(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val amount: Float,
    val relatedTxId: String,
    val isRead: Boolean,
    val createdAt: String
)
