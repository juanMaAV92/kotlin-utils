package com.juanmaav.platform.logger

/**
 * Bridge to an external tracing system (e.g. OpenTelemetry). Return `null` when there is
 * no active trace.
 */
public fun interface TraceProvider {
    public fun currentTrace(): TraceInfo?
}

public data class TraceInfo(
    val traceId: String,
    val spanId: String,
)
