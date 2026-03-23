package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import io.smallrye.mutiny.Uni
import org.jboss.logging.Logger

class FlowEngine<T : FlowContext>(private val logger: Logger) {
    fun run(context: T, steps: List<Step<T>>): Uni<T> {
        val executedSteps = mutableListOf<Step<T>>()
        var chain = Uni.createFrom().item(context)

        steps.forEach { step ->
            chain = chain.onItem().transformToUni { ctx ->
                logger.debug("Executing step: ${step::class.simpleName}")
                step.execute(ctx)
                    .ifNoItem().after(step.timeout).failWith { 
                        Exception("Step ${step::class.simpleName} timed out after ${step.timeout}") 
                    }
                    .onItem().invoke { _ -> executedSteps.add(step) }
            }
        }

        return chain.onFailure().recoverWithUni { failure ->
            logger.error("Flow failed: ${failure.message}. Starting compensation.")
            compensate(context, executedSteps.reversed())
                .onItem().transformToUni { _ -> Uni.createFrom().failure<T>(failure) }
        }
    }

    private fun compensate(context: T, steps: List<Step<T>>): Uni<Void> {
        if (steps.isEmpty()) return Uni.createFrom().voidItem()
        var chain = Uni.createFrom().voidItem()
        steps.forEach { step ->
            chain = chain.onItem().transformToUni { _ ->
                logger.debug("Compensating step: ${step::class.simpleName}")
                step.onFailure(context)
                    .onFailure().recoverWithUni(Uni.createFrom().voidItem())
            }
        }
        return chain
    }
}
