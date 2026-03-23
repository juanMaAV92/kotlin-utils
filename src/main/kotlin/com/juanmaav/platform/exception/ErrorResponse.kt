package com.juanmaav.platform.exception

data class ErrorResponse(
    val code: String,
    val messages: List<String>,
    val timestamp: String,
    val details: Map<String, Any> = emptyMap(),
)

data class HttpErrorResponse(
    val code: String,
    val messages: List<String>,
    val timestamp: String,
    val httpStatus: Int,
    val details: Map<String, Any> = emptyMap(),
)
