package com.example.skillexchange.data.model

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val skillRequired: String = "",
    val skillOffered: String = "",
    val description: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
