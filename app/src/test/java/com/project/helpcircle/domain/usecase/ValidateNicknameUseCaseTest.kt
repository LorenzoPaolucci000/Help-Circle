package com.project.helpcircle.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateNicknameUseCaseTest {

    private val validate = ValidateNicknameUseCase()

    @Test
    fun `accepts a nickname within the length and character rules`() {
        assertEquals(NicknameValidationResult.Valid, validate("Wanderer42"))
    }

    @Test
    fun `accepts a nickname at exactly the minimum length`() {
        val nickname = "a".repeat(ValidateNicknameUseCase.MIN_LENGTH)

        assertEquals(NicknameValidationResult.Valid, validate(nickname))
    }

    @Test
    fun `accepts a nickname at exactly the maximum length`() {
        val nickname = "a".repeat(ValidateNicknameUseCase.MAX_LENGTH)

        assertEquals(NicknameValidationResult.Valid, validate(nickname))
    }

    @Test
    fun `rejects a nickname shorter than the minimum length`() {
        val nickname = "a".repeat(ValidateNicknameUseCase.MIN_LENGTH - 1)

        assertEquals(NicknameValidationResult.TooShort, validate(nickname))
    }

    @Test
    fun `rejects a nickname longer than the maximum length`() {
        val nickname = "a".repeat(ValidateNicknameUseCase.MAX_LENGTH + 1)

        assertEquals(NicknameValidationResult.TooLong, validate(nickname))
    }

    @Test
    fun `rejects a nickname containing spaces`() {
        assertEquals(NicknameValidationResult.InvalidCharacters, validate("two words"))
    }

    @Test
    fun `rejects a nickname containing punctuation`() {
        assertEquals(NicknameValidationResult.InvalidCharacters, validate("no-dashes!"))
    }

    @Test
    fun `rejects an email-shaped nickname since it would leak PII`() {
        assertEquals(NicknameValidationResult.InvalidCharacters, validate("user@example.com"))
    }
}
