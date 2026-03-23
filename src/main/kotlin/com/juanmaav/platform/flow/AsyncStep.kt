package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.logger.StructuredLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AsyncStep<T : FlowContext>(
    private val delegate: Step<T>,
    private val scope: CoroutineScope,
    private val logger: StructuredLogger,
) : Step<T> {
    override suspend fun execute(context: T): T {
        scope.launch {
            try {
                delegate.execute(context)
                logger.debug(delegate::class.simpleName ?: "AsyncStep", "Async step completed")
            } catch (e: Exception) {
                logger.error(
                    delegate::class.simpleName ?: "AsyncStep",
                    "Async step failed in background",
                    e,
                    mapOf("traceId" to context.traceId),
                )
            }
        }
        return context
    }

    override suspend fun onFailure(context: T) {
        delegate.onFailure(context)
    }
}
