package edu.utap.repository

import com.google.firebase.FirebaseException
import com.google.firebase.firestore.DocumentSnapshot
import edu.utap.utils.FirebaseErrorMapper

abstract class BaseRepository {

    protected suspend fun <T> safeFirestoreCall(operation: suspend () -> T): Result<T> = try {
        val result = operation()
        Result.success(result)
    } catch (e: FirebaseException) {
        val message = mapFirebaseError(e)
        Result.failure(Exception(message))
    } catch (e: Throwable) {
        Result.failure(e)
    }

    protected suspend fun <T> safeNetworkCall(operation: suspend () -> T): Result<T> = try {
        val result = operation()
        Result.success(result)
    } catch (e: FirebaseException) {
        val message = mapFirebaseError(e)
        Result.failure(Exception(message))
    } catch (e: Throwable) {
        Result.failure(e)
    }

    protected fun mapFirebaseError(exception: Throwable): String =
        FirebaseErrorMapper.getErrorMessage(exception)

    protected inline fun <reified T> DocumentSnapshot.toDomainObject(): T? =
        this.toObject(T::class.java)
}
