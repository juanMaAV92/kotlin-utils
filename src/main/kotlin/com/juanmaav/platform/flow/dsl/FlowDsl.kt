package com.juanmaav.platform.flow.dsl

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.flow.FlowEngine
import com.juanmaav.platform.flow.Step
import io.smallrye.mutiny.Uni
import org.jboss.logging.Logger

class FlowBuilder<T : FlowContext>(private val context: T, private val logger: Logger) {
    private val steps = mutableListOf<Step<T>>()

    fun step(step: Step<T>) {
        steps.add(step)
    }

    fun execute(): Uni<T> {
        return FlowEngine<T>(logger).run(context, steps)
    }
}

fun <T : FlowContext> flow(context: T, logger: Logger, init: FlowBuilder<T>.() -> Unit): Uni<T> {
    val builder = FlowBuilder(context, logger)
    builder.init()
    return builder.execute()
}
