package com.juanmaav.platform.logger

interface StructuredLogger {
    fun fatal(
        step: String,
        message: String,
        attributes: Map<String, Any?> = emptyMap(),
    )

    fun error(
        step: String,
        message: String,
        error: Throwable? = null,
        attributes: Map<String, Any?> = emptyMap(),
    )

    fun warn(
        step: String,
        message: String,
        attributes: Map<String, Any?> = emptyMap(),
    )

    fun info(
        step: String,
        message: String,
        attributes: Map<String, Any?> = emptyMap(),
    )

    fun debug(
        step: String,
        message: String,
        attributes: Map<String, Any?> = emptyMap(),
    )
}
