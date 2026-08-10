package com.project.helpcircle.domain.model

/** An anonymous, login-less identity: a random alphanumeric hash with no personal data. */
@JvmInline
value class UserIdentity(val anonymousHash: String) {
    init {
        require(anonymousHash.isNotBlank()) { "Anonymous hash must not be blank" }
    }
}
