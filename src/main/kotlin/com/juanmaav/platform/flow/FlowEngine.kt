package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.exception.PlatformException
import com.juanmaav.platform.logger.StructuredLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Runs [Step]s sequentially and, when one fails, compensates the executed steps in reverse
 * order (Saga pattern). Compensation runs inside [NonCancellable], so it completes even when
 * a step times out or the calling coroutine is cancelled.
 */
public class FlowEngine<T : FlowContext>(private val logger: StructuredLogger) {
    public suspend fun run(
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
                    withTimeout(step.timeout) {
                        step.execute(currentContext)
                    }
            }
            return currentContext
        } catch (e: TimeoutCancellationException) {
            logFailure(context, e)
            compensateNonCancellable(context, executedSteps)
            throw e
        } catch (e: CancellationException) {
            logger.warn("flow_engine", "Flow cancelled, starting compensation", mapOf("traceId" to context.traceId))
            compensateNonCancellable(context, executedSteps)
            throw e
        } catch (e: Exception) {
            logFailure(context, e)
            compensateNonCancellable(context, executedSteps)
            throw e
        }
    }

    private fun logFailure(
        context: T,
        e: Exception,
    ) {
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
    }

    private suspend fun compensateNonCancellable(
        context: T,
        executedSteps: List<Step<T>>,
    ) {
        withContext(NonCancellable) {
            compensate(context, executedSteps.reversed())
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
                logger.error(stepName, "Compensation failed", e, mapOf("traceId" to context.traceId))
            }
        }
    }
}
