package com.juanmaav.platform.flow.dsl

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.flow.AsyncStep
import com.juanmaav.platform.flow.FlowEngine
import com.juanmaav.platform.flow.ParallelStep
import com.juanmaav.platform.flow.Step
import com.juanmaav.platform.logger.StructuredLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

/** Collects the steps of a flow declared with the [flow] DSL. */
public class FlowBuilder<T : FlowContext>(
    private val context: T,
    private val logger: StructuredLogger,
    private val scope: CoroutineScope,
    private val asyncScope: CoroutineScope? = null,
) {
    private val steps = mutableListOf<Step<T>>()

    /** Adds a sequential step. */
    public fun step(step: Step<T>) {
        steps.add(step)
    }

    /**
     * Adds a background step. It runs in [asyncScope] when one was passed to [flow]
     * (true fire-and-forget); otherwise in the flow's own scope, which awaits it.
     */
    public fun asyncStep(step: Step<T>) {
        steps.add(AsyncStep(step, asyncScope ?: scope, logger))
    }

    /** Adds a block of steps that run concurrently. */
    public fun parallel(init: ParallelBuilder<T>.() -> Unit) {
        val builder = ParallelBuilder<T>()
        builder.init()
        steps.add(ParallelStep(builder.getSteps(), logger))
    }

    public suspend fun execute(): T = FlowEngine<T>(logger).run(context, steps)
}

/** Collects the steps of a [FlowBuilder.parallel] block. */
public class ParallelBuilder<T : FlowContext> {
    private val parallelSteps = mutableListOf<Step<T>>()

    public fun step(step: Step<T>) {
        parallelSteps.add(step)
    }

    public fun getSteps(): List<Step<T>> = parallelSteps
}

/**
 * Declares and runs a flow over [context], returning the final context.
 *
 * Pass [asyncScope] to host [FlowBuilder.asyncStep] work outside the flow's lifecycle:
 * the flow then returns without waiting for async steps, and they survive a flow failure.
 */
public suspend fun <T : FlowContext> flow(
    context: T,
    logger: StructuredLogger,
    asyncScope: CoroutineScope? = null,
    init: FlowBuilder<T>.() -> Unit,
): T =
    coroutineScope {
        val builder = FlowBuilder(context, logger, this, asyncScope)
        builder.init()
        builder.execute()
    }
