package com.example.skillexchange.data.model

import com.google.firebase.Timestamp

data class Swap(
    val id: String = "",
    val userA: String = "",
    val userB: String = "",
    val users: List<String> = emptyList(),
    val skillA: String = "",
    val skillB: String = "",
    val hours: Int = 1,
    val status: String = "pending", // pending, accepted, completed, cancelled
    val confirmedA: Boolean = false,
    val confirmedB: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
)
