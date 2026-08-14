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
    fun `an app is flagged for resolution while nothing has been observed yet`() {
        // The case seen on a real device: the service (re)connected while a monitored app was
        // already on screen, so no window transition ever arrived and every scroll in that app was
        // gated out. The scroll event's own package has to drive the lookup instead.
        val tracker = ForegroundAppTracker()

        assertTrue(tracker.needsForegroundResolution("com.example.social"))
    }

    @Test
    fun `an app is flagged for resolution while a different app is still tracked`() {
        // This tracker outlives the accessibility service (process-wide singleton), so after a
        // service restart it can hold a stale package rather than none at all — checking only for
        // "nothing tracked yet" would miss exactly that case.
        val tracker = ForegroundAppTracker()
        tracker.onForegroundPackageChanged("com.example.mail", atEpochMillis = 1_000, isBlacklisted = false)

        assertTrue(tracker.needsForegroundResolution("com.example.social"))
    }

    @Test
    fun `the already-tracked app needs no further resolution`() {
        val tracker = ForegroundAppTracker()
        tracker.onForegroundPackageChanged("com.example.social", atEpochMillis = 1_000, isBlacklisted = true)

        assertFalse(tracker.needsForegroundResolution("com.example.social"))
    }

    @Test
    fun `resolving an app from its scroll event arms detection exactly as a window transition would`() {
        val tracker = ForegroundAppTracker()

        // What the service now does when a scroll arrives from an app it has no knowledge of.
        assertTrue(tracker.needsForegroundResolution("com.example.social"))
        tracker.onForegroundPackageChanged("com.example.social", atEpochMillis = 5_000, isBlacklisted = true)

        assertFalse(tracker.needsForegroundResolution("com.example.social"))
        assertTrue(tracker.isCurrentForegroundAppBlacklisted)
        assertEquals(5_000L, tracker.lastBlacklistedForegroundAtMillis)
    }

    @Test
    fun `resolving an unmonitored app leaves detection off for it`() {
        val tracker = ForegroundAppTracker()

        tracker.onForegroundPackageChanged("com.example.mail", atEpochMillis = 5_000, isBlacklisted = false)

        // Resolved once, so no repeated lookups, and still correctly gated out.
        assertFalse(tracker.needsForegroundResolution("com.example.mail"))
        assertFalse(tracker.isCurrentForegroundAppBlacklisted)
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
