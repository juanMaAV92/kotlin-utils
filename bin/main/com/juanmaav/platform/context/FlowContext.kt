package com.juanmaav.platform.context

import java.util.UUID

abstract class FlowContext(
    val correlationId: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val tenantId: String? = null,
    val metadata: MutableMap<String, Any> = mutableMapOf(),
) {
    fun addMetadata(
        key: String,
        value: Any,
    ) {
        metadata[key] = value
    }
}
