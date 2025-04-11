package edu.utap.utils

/**
 * Centralized sealed Result wrapper for success and error handling.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null, val code: Int? = null) :
        Result<Nothing>()

    val isSuccess get() = this is Success<T>
    val isError get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Success -> null
        is Error -> cause
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }

    companion object {
        inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: Throwable) {
            Error(
                message = FirebaseErrorMapper.getErrorMessage(e),
                cause = e
            )
        }

        suspend inline fun <T> runCatchingSuspend(crossinline block: suspend () -> T): Result<T> =
            try {
                Success(block())
            } catch (e: Throwable) {
                Error(
                    message = FirebaseErrorMapper.getErrorMessage(e),
                    cause = e
                )
            }
    }
}
