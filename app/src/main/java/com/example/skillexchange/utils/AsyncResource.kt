package com.example.skillexchange.utils

// Extended Resource class with retry capability
sealed class AsyncResource<T>(val data: T? = null, val message: String? = null, val throwable: Throwable? = null) {
    class Success<T>(data: T) : AsyncResource<T>(data)
    class Error<T>(message: String, throwable: Throwable? = null, data: T? = null) : AsyncResource<T>(data, message, throwable)
    class Loading<T>(data: T? = null) : AsyncResource<T>(data)
    class Idle<T> : AsyncResource<T>()
}

// Extension functions for Resource
fun <T> AsyncResource<T>.getDataOrNull(): T? = data

fun <T> AsyncResource<T>.isSuccess(): Boolean = this is AsyncResource.Success

fun <T> AsyncResource<T>.isError(): Boolean = this is AsyncResource.Error

fun <T> AsyncResource<T>.isLoading(): Boolean = this is AsyncResource.Loading

fun <T> AsyncResource<T>.isIdle(): Boolean = this is AsyncResource.Idle

fun <T> AsyncResource<T>.isRetryable(): Boolean {
    return this is AsyncResource.Error && ErrorHandler.isRetryableError(throwable)
}

fun <T, R> AsyncResource<T>.map(transform: (T) -> R): AsyncResource<R> {
    return when (this) {
        is AsyncResource.Success -> AsyncResource.Success(transform(data))
        is AsyncResource.Error -> AsyncResource.Error(message ?: "", throwable)
        is AsyncResource.Loading -> AsyncResource.Loading()
        is AsyncResource.Idle -> AsyncResource.Idle()
    }
}

fun <T> AsyncResource<T>.onSuccess(block: (T) -> Unit): AsyncResource<T> {
    if (this is AsyncResource.Success) {
        block(data)
    }
    return this
}

fun <T> AsyncResource<T>.onError(block: (String, Throwable?) -> Unit): AsyncResource<T> {
    if (this is AsyncResource.Error) {
        block(message ?: "", throwable)
    }
    return this
}

fun <T> AsyncResource<T>.onLoading(block: () -> Unit): AsyncResource<T> {
    if (this is AsyncResource.Loading) {
        block()
    }
    return this
}

fun <T> AsyncResource<T>.getOrNull(): T? = when (this) {
    is AsyncResource.Success -> data
    else -> null
}

fun <T> AsyncResource<T>.getOrThrow(): T = when (this) {
    is AsyncResource.Success -> data
    is AsyncResource.Error -> throw Exception(message, throwable)
    else -> throw IllegalStateException("Resource is not in Success state")
}
