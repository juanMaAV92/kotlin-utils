package com.juanmaav.platform.validation

import com.juanmaav.platform.exception.PlatformException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValidationTest {
    data class User(val name: String, val age: Int, val email: String)

    @Test
    fun `should collect multiple validation errors`() {
        val user = User(name = "", age = 15, email = "invalid-email")

        val ex =
            assertFailsWith<PlatformException> {
                validate(user) {
                    check(value.name.isNotBlank()) { "Name is required" }
                    check(value.age >= 18) { "User must be at least 18" }
                    check(value.email.contains("@")) { "Email must be valid" }
                }
            }

        assertEquals("VALIDATION_FAILED", ex.code)
        assertEquals(3, ex.messages.size)
        assertEquals("Name is required", ex.messages[0])
        assertEquals("User must be at least 18", ex.messages[1])
        assertEquals("Email must be valid", ex.messages[2])
        assertEquals("User", ex.details["target"])
    }

    @Test
    fun `should pass if all conditions are met`() {
        val user = User(name = "Juan", age = 25, email = "juan@example.com")

        // Should not throw
        validate(user) {
            check(value.name.isNotBlank()) { "Name is required" }
            check(value.age >= 18) { "User must be at least 18" }
        }
    }
}
