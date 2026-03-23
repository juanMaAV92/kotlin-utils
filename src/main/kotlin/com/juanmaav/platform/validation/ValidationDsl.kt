package com.juanmaav.platform.validation

import com.juanmaav.platform.exception.PlatformException

class ValidationBuilder<T>(val value: T) {
    private val errors = mutableListOf<String>()

    fun check(
        condition: Boolean,
        message: () -> String,
    ) {
        if (!condition) {
            errors.add(message())
        }
    }

    fun getErrors(): List<String> = errors
}

fun <T> validate(
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
