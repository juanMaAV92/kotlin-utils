package com.juanmaav.platform.retry

import com.juanmaav.platform.exception.HttpException
import com.juanmaav.platform.logger.StructuredLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Runs [block] with exponential backoff, retrying only when [retryIf] accepts the exception
 * (by default [isTransient]). The delay grows by [factor] up to [maxDelay].
 *
 * [CancellationException] is always rethrown immediately — cancellation is never retried.
 */
public suspend fun <T> retry(
    maxAttempts: Int = 3,
    initialDelay: Duration = 100.milliseconds,
    factor: Double = 2.0,
    maxDelay: Duration = 10_000.milliseconds,
    logger: StructuredLogger? = null,
    retryIf: (Exception) -> Boolean = ::isTransient,
    block: suspend () -> T,
): T {
    require(maxAttempts >= 1) { "maxAttempts must be at least 1, was $maxAttempts" }

    var currentDelay = initialDelay

    repeat(maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
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

    error("unreachable: the last attempt either returns or throws")
}

/**
 * Default retry predicate: connection/IO errors and transient HTTP statuses
 * (408, 429, 500, 502, 503, 504). Client errors like 400/401/403 are not retried.
 */
public fun isTransient(e: Exception): Boolean =
    when (e) {
        is IOException -> true
        is HttpException -> e.httpStatus in TRANSIENT_HTTP_CODES
        else -> false
    }

private val TRANSIENT_HTTP_CODES = setOf(408, 429, 500, 502, 503, 504)
