package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.MemberStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The check and the mark are separate calls, and these cases pin why: marking has to be something
 * the caller does only once a write has actually landed.
 */
class PublishedStatusTrackerTest {

    private val tracker = PublishedStatusTracker()

    @Test
    fun `nothing has been published to begin with`() {
        assertNull(tracker.lastPublishedStatus)
        assertFalse(tracker.isUnchanged(MemberStatus.OK))
    }

    @Test
    fun `a status is unchanged only after it has been marked`() {
        assertFalse(tracker.isUnchanged(MemberStatus.CRISIS))

        tracker.markPublished(MemberStatus.CRISIS)

        assertTrue(tracker.isUnchanged(MemberStatus.CRISIS))
        assertFalse(tracker.isUnchanged(MemberStatus.AT_RISK))
    }

    /**
     * The point of splitting check from mark: a caller that checked but could not complete the
     * write must be able to leave the tracker untouched, so the next reading tries again rather
     * than the status being suppressed until it happens to change.
     */
    @Test
    fun `checking alone never records anything`() {
        repeat(5) { tracker.isUnchanged(MemberStatus.CRISIS) }

        assertNull(tracker.lastPublishedStatus)
    }

    @Test
    fun `resetting makes the next reading a fresh transition`() {
        tracker.markPublished(MemberStatus.OK)

        tracker.reset()

        assertNull(tracker.lastPublishedStatus)
        assertFalse(tracker.isUnchanged(MemberStatus.OK))
    }
}
