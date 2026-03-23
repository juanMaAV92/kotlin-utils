package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import io.smallrye.mutiny.Uni
import java.time.Duration

interface Step<T : FlowContext> {
    fun execute(context: T): Uni<T>
    fun onFailure(context: T): Uni<Void> = Uni.createFrom().voidItem()
    val timeout: Duration get() = Duration.ofSeconds(30)
}
