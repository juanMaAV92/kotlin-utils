package com.juanmaav.platform.exception

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformExceptionTest {
    @Test
    fun `should create PlatformException with single message`() {
        val ex = PlatformException("ERR001", "Something failed")
        assertEquals("ERR001", ex.code)
        assertEquals("Something failed", ex.message)
        assertTrue(ex.messages.contains("Something failed"))
    }

    @Test
    fun `should create HttpException with status`() {
        val ex = HttpException("NOT_FOUND", "User not found", 404)
        assertEquals(404, ex.httpStatus)
        assertEquals("NOT_FOUND", ex.code)
    }
}
