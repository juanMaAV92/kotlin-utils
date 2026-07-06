package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.logger.StructuredLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Executes [steps] concurrently and waits for all of them.
 *
 * Sub-steps share the same context instance — they must mutate it (metadata is thread-safe)
 * instead of returning a new one. If any sub-step fails, [FlowEngine] compensates the whole
 * block once: [onFailure] runs every sub-step's compensation concurrently.
 */
public class ParallelStep<T : FlowContext>(
    private val steps: List<Step<T>>,
    private val logger: StructuredLogger? = null,
) : Step<T> {
    override val timeout: Duration = steps.maxOfOrNull { it.timeout } ?: 30.seconds

    override suspend fun execute(context: T): T {
        if (steps.isEmpty()) return context
        coroutineScope {
            steps.map { async { it.execute(context) } }.awaitAll()
        }
        return context
    }

    override suspend fun onFailure(context: T) {
        if (steps.isEmpty()) return
        coroutineScope {
            steps.map { step ->
                async {
                    try {
                        step.onFailure(context)
                    } catch (e: Exception) {
                        val stepName = step::class.simpleName ?: "UnknownStep"
                        logger?.error(stepName, "Compensation failed", e, mapOf("traceId" to context.traceId))
                    }
                }
            }.awaitAll()
        }
    }
}
