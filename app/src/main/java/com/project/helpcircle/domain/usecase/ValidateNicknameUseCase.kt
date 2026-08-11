package com.project.helpcircle.domain.usecase

/** A self-chosen nickname's standing against the length/character rules that keep it a pseudonym, not PII. */
sealed class NicknameValidationResult {
    data object Valid : NicknameValidationResult()
    data object TooShort : NicknameValidationResult()
    data object TooLong : NicknameValidationResult()
    data object InvalidCharacters : NicknameValidationResult()
}

/** Validates a self-chosen nickname: letters/digits only, so it can't carry an email or full name, per the Zero-PII rule. */
class ValidateNicknameUseCase {
    operator fun invoke(nickname: String): NicknameValidationResult = when {
        nickname.length < MIN_LENGTH -> NicknameValidationResult.TooShort
        nickname.length > MAX_LENGTH -> NicknameValidationResult.TooLong
        !nickname.all { it.isLetterOrDigit() } -> NicknameValidationResult.InvalidCharacters
        else -> NicknameValidationResult.Valid
    }

    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 20
    }
}
