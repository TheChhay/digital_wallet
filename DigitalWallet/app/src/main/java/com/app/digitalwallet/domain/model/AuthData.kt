package com.app.digitalwallet.domain.model

data class AuthData(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)
