package com.project.helpcircle.domain.model

import com.project.helpcircle.domain.usecase.NicknameValidationResult
import com.project.helpcircle.domain.usecase.ValidateNicknameUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityNameGeneratorTest {

    private val validate = ValidateNicknameUseCase()

    @Test
    fun `generates names that pass the same validation a typed one must`() {
        repeat(50) {
            val name = CommunityNameGenerator.generate()

            assertEquals("\"$name\" was rejected by the validation the UI gates on", NicknameValidationResult.Valid, validate(name))
        }
    }

    @Test
    fun `generates names ending in a two-digit suffix`() {
        repeat(50) {
            val name = CommunityNameGenerator.generate()
            val suffix = name.takeLast(2)

            assertTrue("\"$name\" doesn't end in two digits", suffix.all { it.isDigit() })
            assertTrue("\"$name\" has a suffix outside 10-99", suffix.toInt() in 10..99)
            assertTrue("\"$name\" has a third trailing digit", !name.dropLast(2).last().isDigit())
        }
    }

    @Test
    fun `generates names that vary across calls`() {
        val names = (1..50).map { CommunityNameGenerator.generate() }.toSet()

        assertTrue("expected varied names across 50 generations, got ${names.size} distinct", names.size > 1)
    }
}
