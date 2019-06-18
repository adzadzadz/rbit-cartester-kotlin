package com.adriansaycon.rbit_cartester.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoggedInUser(
    val token: String,
    val userId: String,
    val displayName: String
)
