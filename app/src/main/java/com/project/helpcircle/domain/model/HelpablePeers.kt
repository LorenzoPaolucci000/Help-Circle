package com.project.helpcircle.domain.model

/**
 * The subset of a community's roster that can actually receive an intervention right now, as shown
 * on the Help screen.
 *
 * Deliberately narrower than [CommunityState.members]: a nudge is a response to someone losing
 * agency, so offering it against a peer who is doing fine would turn a support gesture into
 * unsolicited noise. Only [MemberStatus.AT_RISK] and [MemberStatus.CRISIS] members qualify, and the
 * sender's own roster entry is always excluded — the members subcollection carries every member's
 * document, this device's included, so without that filter a user could nudge themselves.
 */
data class HelpablePeers(
    /** Peers who can be nudged, most urgent first. */
    val peers: List<CommunityMember>,
    /**
     * Every peer in the circle regardless of status, self excluded. Lets the UI tell "you have no
     * peers yet" apart from "your peers are all doing fine", which read very differently to a user
     * even though both produce an empty [peers] list.
     */
    val totalPeerCount: Int
) {
    companion object {
        val EMPTY = HelpablePeers(peers = emptyList(), totalPeerCount = 0)

        /** Filters [members] down to the peers [selfAnonymousId] may currently send a nudge to. */
        fun from(members: List<CommunityMember>, selfAnonymousId: String): HelpablePeers {
            val peers = members.filterNot { it.anonymousId == selfAnonymousId }
            return HelpablePeers(
                peers = peers
                    .filter { it.status == MemberStatus.CRISIS || it.status == MemberStatus.AT_RISK }
                    // Crisis before at-risk, so whoever needs help most is at the top of the list.
                    .sortedBy { if (it.status == MemberStatus.CRISIS) 0 else 1 },
                totalPeerCount = peers.size
            )
        }
    }
}
