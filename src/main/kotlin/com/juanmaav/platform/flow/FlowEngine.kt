package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.exception.PlatformException
import com.juanmaav.platform.logger.StructuredLogger
import kotlinx.coroutines.withTimeout

class FlowEngine<T : FlowContext>(private val logger: StructuredLogger) {
    suspend fun run(
        context: T,
        steps: List<Step<T>>,
    ): T {
        val executedSteps = mutableListOf<Step<T>>()

        try {
            var currentContext = context
            for (step in steps) {
                val stepName = step::class.simpleName ?: "UnknownStep"
                logger.debug(stepName, "Executing step", mapOf("traceId" to context.traceId))

                executedSteps.add(step)
                currentContext =
                    withTimeout(step.timeout.toMillis()) {
                        step.execute(currentContext)
                    }
            }
            return currentContext
        } catch (e: Exception) {
            val attributes =
                mutableMapOf<String, Any?>(
                    "traceId" to context.traceId,
                    "error_message" to e.message,
                )

            if (e is PlatformException) {
                attributes["error_code"] = e.code
                attributes["error_details"] = e.details
            }

            logger.error("flow_engine", "Flow failed, starting compensation", e, attributes)
            compensate(context, executedSteps.reversed())
            throw e
        }
    }

    private suspend fun compensate(
        context: T,
        steps: List<Step<T>>,
    ) {
        for (step in steps) {
            val stepName = step::class.simpleName ?: "UnknownStep"
            try {
                logger.debug(stepName, "Compensating step", mapOf("traceId" to context.traceId))
                step.onFailure(context)
            } catch (e: Exception) {
                logger.error(
                    stepName,
                    "Compensation failed",
                    e,
                    mapOf("traceId" to context.traceId, "original_error" to e.message),
                )
            }
        }
    }
}
