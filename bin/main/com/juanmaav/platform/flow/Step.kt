package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import java.time.Duration

interface Step<T : FlowContext> {
    suspend fun execute(context: T): T

    suspend fun onFailure(context: T) {}

    val timeout: Duration get() = Duration.ofSeconds(30)
}
