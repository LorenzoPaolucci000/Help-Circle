package com.project.helpcircle.domain.model

/** An anonymous, login-less identity: a random alphanumeric hash paired with a self-chosen pseudonym, no personal data. */
data class UserIdentity(
    val anonymousHash: String,
    val nickname: String = ""
) {
    init {
        require(anonymousHash.isNotBlank()) { "Anonymous hash must not be blank" }
    }
}
