package com.juanmaav.platform.exception

import jakarta.ws.rs.core.Response
import java.time.Instant

open class PlatformException(
    val code: String,
    val messages: List<String>,
    val httpStatus: Int = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, Any> = emptyMap()
) : RuntimeException(messages.joinToString("; ")) {
    constructor(code: String, message: String, httpStatus: Int = 500) : 
        this(code, listOf(message), httpStatus)
}
