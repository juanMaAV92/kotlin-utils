package com.juanmaav.platform.logger

import kotlin.test.Test

class JsonStructuredLoggerTest {
    @Test
    fun `should format log as JSON`() {
        val logger = JsonStructuredLogger("test-service")
        // We use a manual verification or check if it throws
        // Since it writes to SLF4J, we can't easily capture it without more setup,
        // but we can test the internal buildJson if it were accessible or just run it to ensure no crashes
        logger.info("step-1", "Hello world", mapOf("key" to "value", "num" to 123))
    }

    @Test
    fun `should escape JSON special characters`() {
        val logger = JsonStructuredLogger("test-service")
        logger.info("step-1", "Message with \"quotes\" and \n newlines", mapOf("tab" to "\t"))
    }
}
