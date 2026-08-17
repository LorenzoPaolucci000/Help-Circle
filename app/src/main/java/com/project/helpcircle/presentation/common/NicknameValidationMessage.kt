package com.project.helpcircle.presentation.common

import com.project.helpcircle.domain.usecase.NicknameValidationResult

/**
 * The supporting text to show under a name field, or null when there's nothing to say.
 *
 * Shared because the same rules govern two different things a user types — their own nickname and
 * the name of a circle they create — and the two screens phrasing the same rejection differently
 * would read as two different rules.
 */
fun nicknameValidationMessage(result: NicknameValidationResult?): String? = when (result) {
    NicknameValidationResult.TooShort -> "At least 3 characters"
    NicknameValidationResult.TooLong -> "At most 20 characters"
    NicknameValidationResult.InvalidCharacters -> "Letters and numbers only"
    NicknameValidationResult.Valid, null -> null
}
