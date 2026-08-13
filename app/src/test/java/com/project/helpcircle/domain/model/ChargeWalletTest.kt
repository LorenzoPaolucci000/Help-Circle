package com.project.helpcircle.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private val ZONE: ZoneId = ZoneId.of("UTC")

private fun atUtc(date: LocalDate, hour: Int, minute: Int = 0): Long =
    LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(ZONE).toInstant().toEpochMilli()

class ChargeWalletTest {

    @Test
    fun `can afford a nudge only when charges cover its cost`() {
        val wallet = ChargeWallet(currentCharges = 2, lastReplenishedAtEpochMillis = 0)

        assertTrue(wallet.canAfford(Nudge.Haptic))
        assertFalse(wallet.canAfford(Nudge.ContentBlur))
    }

    @Test
    fun `spending deducts exactly the nudge's charge cost`() {
        val wallet = ChargeWallet(currentCharges = 5, lastReplenishedAtEpochMillis = 0)

        val spent = wallet.spend(Nudge.Haptic)

        assertEquals(3, spent.currentCharges)
    }

    @Test
    fun `spending on an unaffordable nudge throws`() {
        val wallet = ChargeWallet(currentCharges = 1, lastReplenishedAtEpochMillis = 0)

        assertThrows(IllegalArgumentException::class.java) { wallet.spend(Nudge.Haptic) }
    }

    @Test
    fun `replenishment is a no-op when time hasn't moved forward`() {
        val wallet = ChargeWallet(currentCharges = 3, lastReplenishedAtEpochMillis = 10_000)

        val result = wallet.replenished(nowEpochMillis = 10_000, isFocusMode = false, zoneId = ZONE)

        assertEquals(wallet, result)
    }

    @Test
    fun `replenishment is a no-op before a full baseline interval has elapsed`() {
        val start = atUtc(LocalDate.of(2026, 1, 5), hour = 12)
        val wallet = ChargeWallet(currentCharges = 3, lastReplenishedAtEpochMillis = start)

        val result = wallet.replenished(
            nowEpochMillis = start + ChargeWallet.BASELINE_INTERVAL_MILLIS - 1,
            isFocusMode = false,
            zoneId = ZONE
        )

        assertEquals(wallet, result)
    }

    @Test
    fun `replenishment grants one charge per elapsed baseline hour outside focus mode`() {
        val start = atUtc(LocalDate.of(2026, 1, 5), hour = 12)
        val wallet = ChargeWallet(currentCharges = 3, lastReplenishedAtEpochMillis = start)

        val result = wallet.replenished(
            nowEpochMillis = start + ChargeWallet.BASELINE_INTERVAL_MILLIS,
            isFocusMode = false,
            zoneId = ZONE
        )

        assertEquals(4, result.currentCharges)
        assertEquals(start + ChargeWallet.BASELINE_INTERVAL_MILLIS, result.lastReplenishedAtEpochMillis)
    }

    @Test
    fun `replenishment grants one charge per elapsed 30 minutes in focus mode`() {
        val start = atUtc(LocalDate.of(2026, 1, 5), hour = 12)
        val wallet = ChargeWallet(currentCharges = 3, lastReplenishedAtEpochMillis = start)

        val result = wallet.replenished(
            nowEpochMillis = start + ChargeWallet.FOCUS_MODE_INTERVAL_MILLIS,
            isFocusMode = true,
            zoneId = ZONE
        )

        assertEquals(4, result.currentCharges)
    }

    @Test
    fun `replenishment grants multiple charges for multiple elapsed intervals`() {
        val start = atUtc(LocalDate.of(2026, 1, 5), hour = 0)
        val wallet = ChargeWallet(currentCharges = 2, lastReplenishedAtEpochMillis = start)

        val result = wallet.replenished(
            nowEpochMillis = start + 3 * ChargeWallet.BASELINE_INTERVAL_MILLIS,
            isFocusMode = false,
            zoneId = ZONE
        )

        assertEquals(5, result.currentCharges)
        assertEquals(start + 3 * ChargeWallet.BASELINE_INTERVAL_MILLIS, result.lastReplenishedAtEpochMillis)
    }

    @Test
    fun `replenishment never exceeds the maximum charge cap`() {
        val start = atUtc(LocalDate.of(2026, 1, 5), hour = 0)
        val wallet = ChargeWallet(currentCharges = 9, lastReplenishedAtEpochMillis = start)

        val result = wallet.replenished(
            nowEpochMillis = start + 20 * ChargeWallet.BASELINE_INTERVAL_MILLIS,
            isFocusMode = false,
            zoneId = ZONE
        )

        assertEquals(ChargeWallet.MAX_CHARGES, result.currentCharges)
    }

    @Test
    fun `crossing a local midnight resets to full charges regardless of interval math`() {
        val lastNight = atUtc(LocalDate.of(2026, 1, 5), hour = 23, minute = 30)
        val wallet = ChargeWallet(currentCharges = 2, lastReplenishedAtEpochMillis = lastNight)
        val nextMorning = atUtc(LocalDate.of(2026, 1, 6), hour = 8)

        val result = wallet.replenished(nowEpochMillis = nextMorning, isFocusMode = false, zoneId = ZONE)

        assertEquals(ChargeWallet.MAX_CHARGES, result.currentCharges)
        assertEquals(atUtc(LocalDate.of(2026, 1, 6), hour = 0), result.lastReplenishedAtEpochMillis)
    }

    @Test
    fun `a midnight reset applies even if the wallet was already full`() {
        val lastNight = atUtc(LocalDate.of(2026, 1, 5), hour = 23, minute = 30)
        val wallet = ChargeWallet(currentCharges = ChargeWallet.MAX_CHARGES, lastReplenishedAtEpochMillis = lastNight)
        val nextMorning = atUtc(LocalDate.of(2026, 1, 6), hour = 0, minute = 1)

        val result = wallet.replenished(nowEpochMillis = nextMorning, isFocusMode = false, zoneId = ZONE)

        assertEquals(ChargeWallet.MAX_CHARGES, result.currentCharges)
        assertEquals(atUtc(LocalDate.of(2026, 1, 6), hour = 0), result.lastReplenishedAtEpochMillis)
    }
}
