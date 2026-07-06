package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.exception.PlatformException
import com.juanmaav.platform.flow.dsl.flow
import com.juanmaav.platform.logger.StructuredLogger
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlowEngineTest {
    class TestLogger : StructuredLogger {
        val errorLogs = mutableListOf<String>()
        val errorAttributes = mutableListOf<Map<String, Any?>>()

        override fun fatal(
            step: String,
            message: String,
            attributes: Map<String, Any?>,
        ) {}

        override fun error(
            step: String,
            message: String,
            error: Throwable?,
            attributes: Map<String, Any?>,
        ) {
            errorLogs.add(message)
            errorAttributes.add(attributes)
        }

        override fun warn(
            step: String,
            message: String,
            attributes: Map<String, Any?>,
        ) {}

        override fun info(
            step: String,
            message: String,
            attributes: Map<String, Any?>,
        ) {}

        override fun debug(
            step: String,
            message: String,
            attributes: Map<String, Any?>,
        ) {}
    }

    private val logger = TestLogger()

    class TestContext(userId: String) : FlowContext(userId = userId) {
        var step1Executed = false
        var step2Executed = false
        var compensationExecuted = false
    }

    class Step1 : Step<TestContext> {
        override suspend fun execute(context: TestContext): TestContext {
            context.step1Executed = true
            return context
        }

        override suspend fun onFailure(context: TestContext) {
            context.compensationExecuted = true
        }
    }

    class Step2 : Step<TestContext> {
        override suspend fun execute(context: TestContext): TestContext {
            context.step2Executed = true
            return context
        }
    }

    class FailingStep : Step<TestContext> {
        override suspend fun execute(context: TestContext): TestContext {
            throw RuntimeException("Planned failure")
        }
    }

    @Test
    fun `should execute steps in order`() =
        runTest {
            val context = TestContext("user-1")
            val result =
                flow(context, logger) {
                    step(Step1())
                    step(Step2())
                }

            assertTrue(result.step1Executed)
            assertTrue(result.step2Executed)
        }

    @Test
    fun `should compensate the failing step in sequential execution`() =
        runTest {
            val context = TestContext("user-1")
            var failingStepCompensationCalled = false

            class MyFailingStep : Step<TestContext> {
                override suspend fun execute(context: TestContext): TestContext {
                    throw RuntimeException("Planned failure")
                }

                override suspend fun onFailure(context: TestContext) {
                    failingStepCompensationCalled = true
                }
            }

            try {
                flow(context, logger) {
                    step(MyFailingStep())
                }
            } catch (e: Exception) {
                assertEquals("Planned failure", e.message)
            }

            assertTrue(failingStepCompensationCalled, "Failing step should have been compensated")
        }

    @Test
    fun `should compensate successful steps in parallel block if one fails`() =
        runTest {
            val context = TestContext("user-1")
            try {
                flow(context, logger) {
                    parallel {
                        step(Step1())
                        step(FailingStep())
                    }
                }
            } catch (e: Exception) {
                assertEquals("Planned failure", e.message)
            }

            assertTrue(context.step1Executed, "Step1 should have been executed")
            assertTrue(context.compensationExecuted, "Step1 should have been compensated")
        }

    @Test
    fun `should log PlatformException details on failure`() =
        runTest {
            val context = TestContext("user-1")

            class MyPlatformStep : Step<TestContext> {
                override suspend fun execute(context: TestContext): TestContext {
                    throw PlatformException(
                        "ERR_456",
                        listOf("Business failure"),
                        details = mapOf("reason" to "out of stock"),
                    )
                }
            }

            try {
                flow(context, logger) {
                    step(MyPlatformStep())
                }
            } catch (e: PlatformException) {
                assertEquals("ERR_456", e.code)
            }

            val attrs = logger.errorAttributes.last()
            assertEquals("ERR_456", attrs["error_code"])
            assertEquals(mapOf("reason" to "out of stock"), attrs["error_details"])
            assertEquals(context.traceId, attrs["traceId"])
        }

    @Test
    fun `should compensate parallel siblings exactly once`() =
        runTest {
            val compensations = AtomicInteger(0)

            val okStep =
                object : Step<TestContext> {
                    override suspend fun execute(context: TestContext) = context

                    override suspend fun onFailure(context: TestContext) {
                        compensations.incrementAndGet()
                    }
                }

            runCatching {
                flow(TestContext("user-1"), logger) {
                    parallel {
                        step(okStep)
                        step(FailingStep())
                    }
                }
            }

            assertEquals(1, compensations.get(), "sibling was compensated ${compensations.get()} times")
        }

    @Test
    fun `should run suspending compensation when the flow is cancelled externally`() =
        runTest {
            val compensations = AtomicInteger(0)

            val quickStep =
                object : Step<TestContext> {
                    override suspend fun execute(context: TestContext) = context

                    override suspend fun onFailure(context: TestContext) {
                        delay(50) // a suspending compensation must survive cancellation
                        compensations.incrementAndGet()
                    }
                }
            val slowStep =
                object : Step<TestContext> {
                    override suspend fun execute(context: TestContext): TestContext {
                        delay(10_000)
                        return context
                    }
                }

            val job =
                launch {
                    runCatching {
                        flow(TestContext("user-1"), logger) {
                            step(quickStep)
                            step(slowStep)
                        }
                    }
                }
            runCurrent() // let the flow reach the slow step
            job.cancelAndJoin()

            assertEquals(1, compensations.get(), "compensation should run despite cancellation")
        }

    @Test
    fun `asyncStep with external scope should not block the flow`() =
        runTest {
            var asyncCompleted = false

            val auditStep =
                object : Step<TestContext> {
                    override suspend fun execute(context: TestContext): TestContext {
                        delay(1_000)
                        asyncCompleted = true
                        return context
                    }
                }

            flow(TestContext("user-1"), logger, asyncScope = backgroundScope) {
                asyncStep(auditStep)
            }

            assertFalse(asyncCompleted, "flow should return without waiting for the async step")
            advanceTimeBy(2_000)
            assertTrue(asyncCompleted, "async step should complete in the external scope")
        }
}
