package com.project.helpcircle.presentation.onboarding

import android.content.Context
import com.project.helpcircle.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.random.Random

/** Suggests a safe pseudonym (adjective + nature noun + 2-digit number) from a local word list, no network involved. */
class NicknameGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generate(): String {
        val adjectives = context.resources.getStringArray(R.array.pseudonym_adjectives)
        val nouns = context.resources.getStringArray(R.array.pseudonym_nouns)
        val number = Random.nextInt(MIN_SUFFIX, MAX_SUFFIX)
        return "${adjectives.random()}${nouns.random()}$number"
    }

    companion object {
        private const val MIN_SUFFIX = 10
        private const val MAX_SUFFIX = 100
    }
}
