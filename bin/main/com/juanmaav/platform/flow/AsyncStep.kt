package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger

class AsyncStep<T : FlowContext>(
    private val delegate: Step<T>,
    private val scope: CoroutineScope,
    private val logger: Logger,
) : Step<T> {
    override suspend fun execute(context: T): T {
        scope.launch {
            try {
                delegate.execute(context)
                logger.debug("Async step ${delegate::class.simpleName} completed")
            } catch (e: Exception) {
                logger.error("Async step ${delegate::class.simpleName} failed in background", e)
            }
        }
        return context
    }

    override suspend fun onFailure(context: T) {
        delegate.onFailure(context)
    }
}
