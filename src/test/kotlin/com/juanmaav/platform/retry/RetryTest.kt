package com.juanmaav.platform.retry

import com.juanmaav.platform.exception.HttpException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class RetryTest {
    @Test
    fun `should return result on first attempt`() =
        runTest {
            val result = retry { "Success" }
            assertEquals("Success", result)
        }

    @Test
    fun `should retry on transient exception and succeed`() =
        runTest {
            var attempts = 0
            val result =
                retry(maxAttempts = 3, initialDelay = 1.milliseconds) {
                    attempts++
                    if (attempts < 2) throw java.io.IOException("Transient error")
                    "Success"
                }
            assertEquals("Success", result)
            assertEquals(2, attempts)
        }

    @Test
    fun `should fail after max attempts`() =
        runTest {
            var attempts = 0
            val ex =
                assertFailsWith<java.io.IOException> {
                    retry(maxAttempts = 2, initialDelay = 1.milliseconds) {
                        attempts++
                        throw java.io.IOException("Persistent error")
                    }
                }
            assertEquals("Persistent error", ex.message)
            assertEquals(2, attempts)
        }

    @Test
    fun `should not retry on non-transient exception`() =
        runTest {
            var attempts = 0
            val ex =
                assertFailsWith<HttpException> {
                    retry(maxAttempts = 3, initialDelay = 1.milliseconds) {
                        attempts++
                        throw HttpException("NOT_FOUND", "User not found", 404)
                    }
                }
            assertEquals(404, ex.httpStatus)
            assertEquals(1, attempts) // Should fail immediately
        }

    @Test
    fun `should retry on transient http status`() =
        runTest {
            var attempts = 0
            val result =
                retry(maxAttempts = 2, initialDelay = 1.milliseconds) {
                    attempts++
                    if (attempts == 1) throw HttpException("SERVER_ERROR", "Try again", 500)
                    "Success"
                }
            assertEquals("Success", result)
            assertEquals(2, attempts)
        }
}
