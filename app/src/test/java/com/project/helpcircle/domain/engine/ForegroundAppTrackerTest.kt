package com.project.helpcircle.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppTrackerTest {

    @Test
    fun `before any foreground app is observed, nothing is considered blacklisted`() {
        val tracker = ForegroundAppTracker()

        assertFalse(tracker.isCurrentForegroundAppBlacklisted)
        assertNull(tracker.currentPackageName)
        assertNull(tracker.lastBlacklistedForegroundAtMillis)
    }

    @Test
    fun `a monitored app in the foreground is reported as blacklisted`() {
        val tracker = ForegroundAppTracker()

        tracker.onForegroundPackageChanged("com.example.social", atEpochMillis = 1_000, isBlacklisted = true)

        assertTrue(tracker.isCurrentForegroundAppBlacklisted)
        assertEquals("com.example.social", tracker.currentPackageName)
        assertEquals(1_000L, tracker.lastBlacklistedForegroundAtMillis)
    }

    @Test
    fun `an unmonitored app in the foreground is not reported as blacklisted`() {
        val tracker = ForegroundAppTracker()

        tracker.onForegroundPackageChanged("com.example.mail", atEpochMillis = 1_000, isBlacklisted = false)

        assertFalse(tracker.isCurrentForegroundAppBlacklisted)
        assertEquals("com.example.mail", tracker.currentPackageName)
        assertNull(tracker.lastBlacklistedForegroundAtMillis)
    }

    @Test
    fun `switching from a monitored app to an unmonitored one clears the blacklisted flag`() {
        val tracker = ForegroundAppTracker()
        tracker.onForegroundPackageChanged("com.example.social", atEpochMillis = 1_000, isBlacklisted = true)

        tracker.onForegroundPackageChanged("com.example.mail", atEpochMillis = 2_000, isBlacklisted = false)

        assertFalse(tracker.isCurrentForegroundAppBlacklisted)
        assertEquals("com.example.mail", tracker.currentPackageName)
        // The last-blacklisted timestamp is a historical record, not cleared by leaving that app.
        assertEquals(1_000L, tracker.lastBlacklistedForegroundAtMillis)
    }
}
