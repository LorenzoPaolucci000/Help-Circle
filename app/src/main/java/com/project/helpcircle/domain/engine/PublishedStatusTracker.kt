package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.MemberStatus

/**
 * Remembers the coarse [MemberStatus] this device last shared with its circle, so the status is
 * published only when it actually changes rather than on every scroll event.
 *
 * That guard is the whole point of this class. Detection re-evaluates on every single
 * `TYPE_VIEW_SCROLLED` the accessibility service forwards, and a run of scrolls reports the same
 * state over and over before it ever transitions — so publishing unconditionally would mean a
 * Firestore write (and, once push is wired up, a notification to every peer) per scroll, which is
 * both a battery problem and a way to bury a circle in alerts.
 *
 * In-memory only, like [ForegroundAppTracker] and [CrisisEpisodeTracker]'s own state: losing it on
 * process death simply means the next transition republishes a status the server already holds,
 * which is harmless. The field is `@Volatile` for the same reason as [ForegroundAppTracker]'s —
 * it's written from the background dispatcher the accessibility service launches its work on, and
 * a stale read here would either suppress a genuine crisis or re-publish one that hasn't changed.
 */
class PublishedStatusTracker {
    @Volatile
    var lastPublishedStatus: MemberStatus? = null
        private set

    /** Whether [status] is already what the circle was last told, i.e. there is nothing to write. */
    fun isUnchanged(status: MemberStatus): Boolean = lastPublishedStatus == status

    /**
     * Records [status] as shared. Deliberately separate from [isUnchanged] so the caller can mark
     * it only *after* the write has actually landed: marking optimistically would mean a failed
     * write, or one attempted while the user belongs to no circle yet, permanently suppresses that
     * status until the tier happens to change again.
     */
    fun markPublished(status: MemberStatus) {
        lastPublishedStatus = status
    }

    /** Forgets the published status, so the next reading is treated as a fresh transition. */
    fun reset() {
        lastPublishedStatus = null
    }
}
