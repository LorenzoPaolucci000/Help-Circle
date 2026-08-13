package com.project.helpcircle.presentation.onboarding

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.helpcircle.R
import com.project.helpcircle.domain.usecase.NicknameValidationResult
import com.project.helpcircle.domain.usecase.ValidateNicknameUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NicknameGeneratorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val generator = NicknameGenerator(context)
    private val validate = ValidateNicknameUseCase()

    @Test
    fun generatedNicknamePassesValidation() {
        repeat(50) {
            val nickname = generator.generate()

            assertEquals(NicknameValidationResult.Valid, validate(nickname))
        }
    }

    @Test
    fun generatedNicknameEndsInATwoDigitNumber() {
        repeat(50) {
            val nickname = generator.generate()

            val suffix = nickname.takeLastWhile { it.isDigit() }
            assertTrue("expected a numeric suffix in \"$nickname\"", suffix.isNotEmpty())
            val number = suffix.toInt()
            assertTrue("suffix $number out of the [10, 100) range", number in 10 until 100)
        }
    }

    @Test
    fun generatedNicknameCombinesAWordListAdjectiveAndNoun() {
        val adjectives = context.resources.getStringArray(R.array.pseudonym_adjectives)
        val nouns = context.resources.getStringArray(R.array.pseudonym_nouns)

        repeat(50) {
            val nickname = generator.generate()
            val withoutSuffix = nickname.dropLastWhile { it.isDigit() }

            val matchesKnownPair = adjectives.any { adjective ->
                nouns.any { noun -> withoutSuffix == "$adjective$noun" }
            }
            assertTrue("\"$nickname\" isn't a known adjective+noun pair", matchesKnownPair)
        }
    }
}
