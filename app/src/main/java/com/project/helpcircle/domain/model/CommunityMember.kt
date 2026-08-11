package com.project.helpcircle.domain.model

/** A community peer as shown to other members: a pseudonym and coarse status only, never PII or a raw IA_ind score. */
data class CommunityMember(
    val anonymousId: String,
    val nickname: String,
    val status: MemberStatus
)
