package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Duration

class ParallelStep<T : FlowContext>(
    private val steps: List<Step<T>>,
) : Step<T> {
    override val timeout: Duration = steps.maxOfOrNull { it.timeout } ?: Duration.ofSeconds(30)

    override suspend fun execute(context: T): T {
        if (steps.isEmpty()) return context
        try {
            coroutineScope {
                steps.map { async { it.execute(context) } }.awaitAll()
            }
        } catch (e: Exception) {
            onFailure(context)
            throw e
        }
        return context
    }

    override suspend fun onFailure(context: T) {
        if (steps.isEmpty()) return
        coroutineScope {
            steps.map {
                async {
                    try {
                        it.onFailure(context)
                    } catch (e: Exception) {
                        // Individual compensation failures are logged by FlowEngine
                    }
                }
            }.awaitAll()
        }
    }
}
