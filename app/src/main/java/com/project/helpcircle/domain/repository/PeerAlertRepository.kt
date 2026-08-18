package com.project.helpcircle.domain.repository

/**
 * Asks the circle's alert service to push a "someone needs support" message to the other members.
 *
 * Separate from [CommunityRepository] because it isn't Firestore: push messages cannot be sent from
 * a client at all, so this crosses to a small external sender that holds the credential instead.
 * Nothing about the user is passed to it beyond which circle to notify — the sender identifies the
 * caller from their own auth token and reads the nickname from the roster itself.
 */
interface PeerAlertRepository {
    /**
     * Best-effort by contract: implementations report failure by doing nothing rather than by
     * throwing, because this is called from the detection path and a circle that cannot be reached
     * must never interfere with detecting or scoring locally.
     */
    suspend fun alertCircle(communityId: String)
}
