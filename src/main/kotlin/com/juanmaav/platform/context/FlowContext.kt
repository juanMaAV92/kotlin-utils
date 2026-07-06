package com.juanmaav.platform.context

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Base context shared by every step of a flow. Carries traceability fields plus a
 * thread-safe [metadata] map, so parallel steps can mutate it concurrently.
 *
 * The [metadata] passed to the constructor is copied — later changes to the original
 * map are not reflected.
 */
public abstract class FlowContext(
    public val traceId: String = UUID.randomUUID().toString(),
    public val userId: String? = null,
    public val tenantId: String? = null,
    metadata: Map<String, Any> = emptyMap(),
) {
    public val metadata: MutableMap<String, Any> = ConcurrentHashMap(metadata)

    public fun addMetadata(
        key: String,
        value: Any,
    ) {
        metadata[key] = value
    }
}
