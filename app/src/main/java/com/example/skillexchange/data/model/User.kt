package com.example.skillexchange.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val skillsOffered: List<String> = emptyList(),
    val skillsNeeded: List<String> = emptyList(),
    val trustScore: Int = 0,
    val skillPoints: Int = 10 // Start with some default points
)
