package com.juanmaav.platform.logger

/**
 * Structured logging contract: every entry has a step name, a message and flat attributes.
 * [JsonStructuredLogger] is the provided implementation.
 */
public interface StructuredLogger {
    public fun fatal(
        step: String,
        message: String,
        attributes: Map<String, Any?> = emptyMap(),
    )

    public fun error(
        step: String,
        message: String,
        error: Throwable? = null,
        attributes: Map<String, Any?> = emptyMap(),
    )

    public fun warn(
        step: String,
        message: String,
        attributes: Map<String, Any?> = emptyMap(),
    )

    public fun info(
        step: String,
        message: String,
        attributes: Map<String, Any?> = emptyMap(),
    )

    public fun debug(
        step: String,
        message: String,
        attributes: Map<String, Any?> = emptyMap(),
    )
}
