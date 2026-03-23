package com.juanmaav.platform.flow.dsl

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.flow.FlowEngine
import com.juanmaav.platform.flow.Step
import com.juanmaav.platform.flow.AsyncStep
import com.juanmaav.platform.flow.ParallelStep
import io.smallrye.mutiny.Uni
import org.jboss.logging.Logger

class FlowBuilder<T : FlowContext>(private val context: T, private val logger: Logger) {
    private val steps = mutableListOf<Step<T>>()

    fun step(step: Step<T>) {
        steps.add(step)
    }

    fun asyncStep(step: Step<T>) {
        steps.add(AsyncStep(step, logger))
    }

    fun parallel(init: ParallelBuilder<T>.() -> Unit) {
        val builder = ParallelBuilder<T>()
        builder.init()
        steps.add(ParallelStep(builder.getSteps()))
    }

    fun execute(): Uni<T> {
        return FlowEngine<T>(logger).run(context, steps)
    }
}

class ParallelBuilder<T : FlowContext> {
    private val parallelSteps = mutableListOf<Step<T>>()
    fun step(step: Step<T>) {
        parallelSteps.add(step)
    }
    fun getSteps(): List<Step<T>> = parallelSteps
}

fun <T : FlowContext> flow(context: T, logger: Logger, init: FlowBuilder<T>.() -> Unit): Uni<T> {
    val builder = FlowBuilder(context, logger)
    builder.init()
    return builder.execute()
}
