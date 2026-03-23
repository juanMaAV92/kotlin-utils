package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import io.smallrye.mutiny.Uni
import org.jboss.logging.Logger

class AsyncStep<T : FlowContext>(
    private val delegate: Step<T>,
    private val logger: Logger
) : Step<T> {
    override fun execute(context: T): Uni<T> {
        delegate.execute(context)
            .onFailure().invoke { error ->
                logger.error("Async step ${delegate::class.simpleName} failed in background", error)
            }
            .subscribe().with { _ -> 
                logger.debug("Async step ${delegate::class.simpleName} completed")
            }
        return Uni.createFrom().item(context)
    }
}
