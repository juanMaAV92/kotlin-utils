package com.juanmaav.platform.exception

/** Standardised error payload produced by [PlatformException.toErrorResponse]. */
public data class ErrorResponse(
    val code: String,
    val messages: List<String>,
    val timestamp: String,
    val details: Map<String, Any> = emptyMap(),
)

/** [ErrorResponse] variant with the HTTP status, produced by [HttpException.toHttpErrorResponse]. */
public data class HttpErrorResponse(
    val code: String,
    val messages: List<String>,
    val timestamp: String,
    val httpStatus: Int,
    val details: Map<String, Any> = emptyMap(),
)
