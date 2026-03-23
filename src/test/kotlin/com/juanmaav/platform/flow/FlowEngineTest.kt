package com.juanmaav.platform.flow

import com.juanmaav.platform.context.FlowContext
import com.juanmaav.platform.flow.dsl.flow
import kotlinx.coroutines.test.runTest
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlowEngineTest {
    private val logger = LoggerFactory.getLogger(FlowEngineTest::class.java)

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
}
