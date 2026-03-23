package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import io.smallrye.mutiny.Uni
import java.time.Duration

class ParallelStep<T : FlowContext>(
    private val steps: List<Step<T>>
) : Step<T> {
    override val timeout: Duration = steps.maxOfOrNull { it.timeout } ?: Duration.ofSeconds(30)

    override fun execute(context: T): Uni<T> {
        if (steps.isEmpty()) return Uni.createFrom().item(context)
        val unis = steps.map { it.execute(context) }
        return Uni.join().all(unis).andCollectFailures().replaceWith(context)
    }

    override fun onFailure(context: T): Uni<Void> {
        if (steps.isEmpty()) return Uni.createFrom().voidItem()
        val unis = steps.map { it.onFailure(context) }
        return Uni.join().all(unis).andCollectFailures().replaceWithVoid()
    }
}
