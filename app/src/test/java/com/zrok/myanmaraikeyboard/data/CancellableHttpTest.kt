package com.zrok.myanmaraikeyboard.data

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CancellableHttpTest {
    @Test fun cancellationDisconnectsBlockedRequestAndDiscardsLateResult() = runBlocking {
        val started = CountDownLatch(1)
        val disconnected = CountDownLatch(1)
        val connection = object : HttpURLConnection(URL("http://localhost/unused")) {
            override fun connect() = Unit
            override fun usingProxy() = false
            override fun disconnect() { disconnected.countDown() }
        }
        var applied = false
        val job = launch(Dispatchers.IO) {
            cancellableHttp(connection) {
                started.countDown()
                check(disconnected.await(3, TimeUnit.SECONDS))
                "late translation"
            }
            applied = true
        }
        assertTrue(started.await(3, TimeUnit.SECONDS))
        job.cancel()
        assertTrue(disconnected.await(1, TimeUnit.SECONDS))
        job.join()
        assertFalse(applied)
    }
}
