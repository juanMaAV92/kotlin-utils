package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger

class FlowEngine<T : FlowContext>(private val logger: Logger) {
    suspend fun run(
        context: T,
        steps: List<Step<T>>,
    ): T {
        val executedSteps = mutableListOf<Step<T>>()

        try {
            var currentContext = context
            for (step in steps) {
                logger.debug("Executing step: ${step::class.simpleName}")
                executedSteps.add(step)
                currentContext =
                    withTimeout(step.timeout.toMillis()) {
                        step.execute(currentContext)
                    }
            }
            return currentContext
        } catch (e: Exception) {
            logger.error("Flow failed: ${e.message}. Starting compensation.")
            compensate(context, executedSteps.reversed())
            throw e
        }
    }

    private suspend fun compensate(
        context: T,
        steps: List<Step<T>>,
    ) {
        for (step in steps) {
            try {
                logger.debug("Compensating step: ${step::class.simpleName}")
                step.onFailure(context)
            } catch (e: Exception) {
                logger.warn("Compensation failed for ${step::class.simpleName}: ${e.message}")
            }
        }
    }
}
