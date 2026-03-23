package com.juanmaav.platform.exception

import java.time.Instant

open class PlatformException(
    val code: String,
    val messages: List<String>,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, Any> = emptyMap(),
) : RuntimeException(messages.joinToString("; ")) {
    constructor(code: String, message: String) :
        this(code, listOf(message))

    fun toErrorResponse() =
        ErrorResponse(
            code = code,
            messages = messages,
            timestamp = timestamp.toString(),
            details = details,
        )
}

open class HttpException(
    code: String,
    messages: List<String>,
    val httpStatus: Int = 500,
    details: Map<String, Any> = emptyMap(),
) : PlatformException(code, messages, details = details) {
    constructor(code: String, message: String, httpStatus: Int = 500) :
        this(code, listOf(message), httpStatus)

    fun toHttpErrorResponse() =
        HttpErrorResponse(
            code = code,
            messages = messages,
            timestamp = timestamp.toString(),
            httpStatus = httpStatus,
            details = details,
        )
}

class ForbiddenException(
    message: String = "Insufficient permissions",
    details: Map<String, Any> = emptyMap(),
) : HttpException("FORBIDDEN", listOf(message), 403, details)

class UnauthorizedException(
    message: String = "Authentication required",
    details: Map<String, Any> = emptyMap(),
) : HttpException("UNAUTHORIZED", listOf(message), 401, details)
