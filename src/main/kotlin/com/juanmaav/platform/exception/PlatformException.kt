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
}

class HttpException(
    code: String,
    messages: List<String>,
    val httpStatus: Int = 500,
    details: Map<String, Any> = emptyMap(),
) : PlatformException(code, messages, details = details) {
    constructor(code: String, message: String, httpStatus: Int = 500) :
        this(code, listOf(message), httpStatus)
}
