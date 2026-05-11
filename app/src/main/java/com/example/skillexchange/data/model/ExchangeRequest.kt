package com.example.skillexchange.data.model

import com.google.firebase.Timestamp

data class ExchangeRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val skillId: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val timestamp: Timestamp = Timestamp.now()
)
