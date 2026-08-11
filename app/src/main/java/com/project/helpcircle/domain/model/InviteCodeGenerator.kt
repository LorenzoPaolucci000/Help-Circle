package com.project.helpcircle.domain.model

/** Generates short, human-shareable invite codes for a newly created community. */
object InviteCodeGenerator {
    private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val LENGTH = 6

    fun generate(): String = (1..LENGTH).map { CHARSET.random() }.joinToString(separator = "")
}
