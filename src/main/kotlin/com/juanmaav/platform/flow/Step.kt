package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A single unit of work inside a flow.
 *
 * Steps receive the shared context, mutate it, and return it. When a later step fails,
 * [onFailure] is invoked in reverse order to compensate (Saga pattern).
 *
 * Steps executed inside a [ParallelStep] or [AsyncStep] must mutate the context in place —
 * a different instance returned from [execute] is ignored there.
 */
public interface Step<T : FlowContext> {
    /** Executes the step. The returned context is passed to the next sequential step. */
    public suspend fun execute(context: T): T

    /** Compensates this step after a downstream failure. Should be idempotent. */
    public suspend fun onFailure(context: T) {}

    /** Maximum time [execute] may run before the flow fails and compensation starts. */
    public val timeout: Duration get() = 30.seconds
}
