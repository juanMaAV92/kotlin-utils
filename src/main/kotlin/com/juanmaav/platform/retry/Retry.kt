package com.juanmaav.platform.retry

import com.juanmaav.platform.exception.HttpException
import com.juanmaav.platform.logger.StructuredLogger
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

suspend fun <T> retry(
    maxAttempts: Int = 3,
    initialDelay: Duration = 100.milliseconds,
    factor: Double = 2.0,
    maxDelay: Duration = 10_000.milliseconds,
    logger: StructuredLogger? = null,
    retryIf: (Exception) -> Boolean = ::isTransient,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelay
    var lastException: Exception? = null

    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e

            if (!retryIf(e)) throw e
            if (attempt == maxAttempts - 1) throw e

            logger?.warn(
                "retry",
                "Attempt ${attempt + 1}/$maxAttempts failed, retrying in $currentDelay",
                mapOf(
                    "attempt" to attempt + 1,
                    "max_attempts" to maxAttempts,
                    "delay_ms" to currentDelay.inWholeMilliseconds,
                    "error_type" to e::class.simpleName,
                    "error_message" to e.message,
                ),
            )

            delay(currentDelay)
            currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
        }
    }

    throw lastException!!
}

fun isTransient(e: Exception): Boolean =
    when (e) {
        is SocketTimeoutException -> true
        is ConnectException -> true
        is IOException -> true
        is HttpException -> e.httpStatus in TRANSIENT_HTTP_CODES
        else -> false
    }

private val TRANSIENT_HTTP_CODES = setOf(408, 429, 500, 502, 503, 504)
