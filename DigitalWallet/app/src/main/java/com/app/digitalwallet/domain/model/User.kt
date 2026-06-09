package com.app.digitalwallet.domain.model

data class User(
    val id: String,
    val phone: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val status: String,
    val profileImageUrl: String? = null
)
