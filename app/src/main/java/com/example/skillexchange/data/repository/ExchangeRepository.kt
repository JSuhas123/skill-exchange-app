package com.example.skillexchange.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun requestExchange(skillId: String, userId: String) {
        // Implementation for requesting a skill exchange in Firestore
    }
}
