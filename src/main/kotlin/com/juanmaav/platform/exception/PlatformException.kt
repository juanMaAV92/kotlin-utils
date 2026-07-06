package com.juanmaav.platform.exception

import java.time.Instant

/**
 * Base exception with a stable error [code], one or more [messages] and free-form [details].
 * Pass [cause] when wrapping another exception to preserve the original chain.
 */
public open class PlatformException(
    public val code: String,
    public val messages: List<String>,
    public val timestamp: Instant = Instant.now(),
    public val details: Map<String, Any> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(messages.joinToString("; "), cause) {
    public constructor(code: String, message: String, cause: Throwable? = null) :
        this(code, listOf(message), cause = cause)

    public fun toErrorResponse(): ErrorResponse =
        ErrorResponse(
            code = code,
            messages = messages,
            timestamp = timestamp.toString(),
            details = details,
        )
}

/** [PlatformException] carrying an HTTP status, for APIs. */
public open class HttpException(
    code: String,
    messages: List<String>,
    public val httpStatus: Int = 500,
    details: Map<String, Any> = emptyMap(),
    cause: Throwable? = null,
) : PlatformException(code, messages, details = details, cause = cause) {
    public constructor(code: String, message: String, httpStatus: Int = 500, cause: Throwable? = null) :
        this(code, listOf(message), httpStatus, cause = cause)

    public fun toHttpErrorResponse(): HttpErrorResponse =
        HttpErrorResponse(
            code = code,
            messages = messages,
            timestamp = timestamp.toString(),
            httpStatus = httpStatus,
            details = details,
        )
}

public class ForbiddenException(
    message: String = "Insufficient permissions",
    details: Map<String, Any> = emptyMap(),
) : HttpException("FORBIDDEN", listOf(message), 403, details)

public class UnauthorizedException(
    message: String = "Authentication required",
    details: Map<String, Any> = emptyMap(),
) : HttpException("UNAUTHORIZED", listOf(message), 401, details)
