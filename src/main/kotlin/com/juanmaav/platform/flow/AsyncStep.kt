package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.logger.StructuredLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Wraps a [Step] so it runs in the background: [execute] launches the delegate in [scope]
 * and returns immediately. The delegate's [Step.timeout] is enforced on the background work,
 * and its failures are logged — they never fail the flow.
 *
 * When [scope] is the flow's own scope (the DSL default), the flow still awaits completion
 * before returning and cancels the work if the flow fails. Pass an external scope through
 * the DSL for true fire-and-forget.
 *
 * If the flow fails afterwards, [onFailure] first cancels the background work (waiting for
 * it to stop) and then compensates the delegate — so execute and compensation never overlap.
 */
public class AsyncStep<T : FlowContext>(
    private val delegate: Step<T>,
    private val scope: CoroutineScope,
    private val logger: StructuredLogger,
) : Step<T> {
    @Volatile
    private var job: Job? = null

    override suspend fun execute(context: T): T {
        val stepName = delegate::class.simpleName ?: "AsyncStep"
        job =
            scope.launch {
                try {
                    withTimeout(delegate.timeout) {
                        delegate.execute(context)
                    }
                    logger.debug(stepName, "Async step completed", mapOf("traceId" to context.traceId))
                } catch (e: TimeoutCancellationException) {
                    logger.error(stepName, "Async step timed out", e, mapOf("traceId" to context.traceId))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error(stepName, "Async step failed in background", e, mapOf("traceId" to context.traceId))
                }
            }
        return context
    }

    override suspend fun onFailure(context: T) {
        job?.cancelAndJoin()
        delegate.onFailure(context)
    }
}
