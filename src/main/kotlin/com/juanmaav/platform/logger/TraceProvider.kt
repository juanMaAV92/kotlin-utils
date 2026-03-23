package com.juanmaav.platform.logger

fun interface TraceProvider {
    fun currentTrace(): TraceInfo?
}

data class TraceInfo(
    val traceId: String,
    val spanId: String,
)
