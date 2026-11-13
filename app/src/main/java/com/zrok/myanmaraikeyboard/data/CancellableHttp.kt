package com.zrok.myanmaraikeyboard.data

import java.net.HttpURLConnection
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Called on Dispatchers.IO. Cancellation disconnects a blocked request immediately. */
internal suspend fun <T> cancellableHttp(connection: HttpURLConnection, block: () -> T): T =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { connection.disconnect() }
        try {
            if (continuation.isActive) {
                val value = block()
                continuation.resume(value)
            }
        } catch (error: Throwable) {
            continuation.resumeWithException(error)
        } finally {
            connection.disconnect()
        }
    }
