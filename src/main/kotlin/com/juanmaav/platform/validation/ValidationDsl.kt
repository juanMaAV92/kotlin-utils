package com.juanmaav.platform.validation

import com.juanmaav.platform.exception.PlatformException

/** Accumulates failed [check]s instead of failing fast. */
public class ValidationBuilder<T>(public val value: T) {
    private val errors = mutableListOf<String>()

    public fun check(
        condition: Boolean,
        message: () -> String,
    ) {
        if (!condition) {
            errors.add(message())
        }
    }

    public fun getErrors(): List<String> = errors
}

/**
 * Validates [value] with the given [block]. Every failed check is collected; if any failed,
 * a single [PlatformException] with code `VALIDATION_FAILED` and all messages is thrown.
 */
public fun <T> validate(
    value: T,
    block: ValidationBuilder<T>.() -> Unit,
) {
    val builder = ValidationBuilder(value)
    builder.block()
    val errors = builder.getErrors()
    if (errors.isNotEmpty()) {
        throw PlatformException(
            code = "VALIDATION_FAILED",
            messages = errors,
            details = mapOf("target" to (value?.let { it::class.simpleName } ?: "null")),
        )
    }
}
