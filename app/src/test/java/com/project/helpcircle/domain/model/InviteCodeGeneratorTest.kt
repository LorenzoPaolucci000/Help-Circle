package com.project.helpcircle.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeGeneratorTest {

    @Test
    fun `generates a 6-character code`() {
        assertEquals(6, InviteCodeGenerator.generate().length)
    }

    @Test
    fun `generates only uppercase letters and digits`() {
        repeat(100) {
            val code = InviteCodeGenerator.generate()

            assertTrue("\"$code\" contains a character outside A-Z0-9", code.matches(Regex("^[A-Z0-9]{6}$")))
        }
    }

    @Test
    fun `generates codes that vary across calls`() {
        val codes = (1..50).map { InviteCodeGenerator.generate() }.toSet()

        assertTrue("expected varied codes across 50 generations, got ${codes.size} distinct", codes.size > 1)
    }
}
