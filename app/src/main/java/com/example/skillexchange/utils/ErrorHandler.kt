package com.example.skillexchange.utils

import kotlinx.coroutines.delay

object ErrorHandler {
    data class RetryConfig(
        val maxRetries: Int = 3,
        val initialDelayMs: Long = 1000,
        val maxDelayMs: Long = 10000,
        val backoffMultiplier: Float = 2f
    )
    
    suspend inline fun <T> withRetry(
        config: RetryConfig = RetryConfig(),
        operation: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null
        var delayMs = config.initialDelayMs
        
        repeat(config.maxRetries) { attempt ->
            try {
                val result = operation()
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                if (attempt < config.maxRetries - 1) {
                    delay(delayMs)
                    delayMs = (delayMs * config.backoffMultiplier).toLong().coerceAtMost(config.maxDelayMs)
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Unknown error"))
    }
    
    fun isNetworkError(throwable: Throwable?): Boolean {
        return throwable?.message?.contains("network", ignoreCase = true) == true ||
               throwable?.message?.contains("connection", ignoreCase = true) == true ||
               throwable?.message?.contains("timeout", ignoreCase = true) == true
    }
    
    fun isRetryableError(throwable: Throwable?): Boolean {
        return isNetworkError(throwable) ||
               throwable?.message?.contains("unavailable", ignoreCase = true) == true ||
               throwable?.message?.contains("temporarily", ignoreCase = true) == true
    }
    
    fun getErrorMessage(throwable: Throwable?): String {
        return when {
            isNetworkError(throwable) -> "Network connection failed. Please check your internet."
            throwable?.message?.contains("permission", ignoreCase = true) == true -> "Permission denied. Check your account access."
            throwable?.message?.contains("not found", ignoreCase = true) == true -> "Resource not found."
            throwable?.message?.contains("already exists", ignoreCase = true) == true -> "This item already exists."
            else -> throwable?.message ?: "An unexpected error occurred."
        }
    }
}
