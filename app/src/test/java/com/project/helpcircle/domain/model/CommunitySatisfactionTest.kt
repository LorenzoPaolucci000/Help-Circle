package com.project.helpcircle.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val THIS_WEEK = 1_000_000L
private const val LAST_WEEK = THIS_WEEK - 7L * 24 * 60 * 60 * 1000

private fun member(
    id: String,
    satisfaction: WeeklySatisfaction? = null,
    weekStart: Long? = null
) = CommunityMember(
    anonymousId = id,
    nickname = id,
    status = MemberStatus.OK,
    agencyScore = 50,
    satisfaction = satisfaction,
    satisfactionWeekStartEpochMillis = weekStart
)

class CommunitySatisfactionTest {

    @Test
    fun `an empty community has no ratings`() {
        val satisfaction = CommunitySatisfaction.from(emptyList(), THIS_WEEK)

        assertFalse(satisfaction.hasRatings)
        assertEquals(0, satisfaction.ratedMemberCount)
        assertEquals(0, satisfaction.memberCount)
    }

    @Test
    fun `members who have never rated count toward the total but not the average`() {
        val members = listOf(member("a"), member("b"))

        val satisfaction = CommunitySatisfaction.from(members, THIS_WEEK)

        assertFalse(satisfaction.hasRatings)
        assertEquals(0, satisfaction.ratedMemberCount)
        assertEquals(2, satisfaction.memberCount)
    }

    @Test
    fun `the average is taken over the rated members only, not the whole circle`() {
        val members = listOf(
            member("a", WeeklySatisfaction.HAPPY, THIS_WEEK),
            member("b", WeeklySatisfaction.HAPPY, THIS_WEEK),
            member("c"),
            member("d")
        )

        val satisfaction = CommunitySatisfaction.from(members, THIS_WEEK)

        assertTrue(satisfaction.hasRatings)
        assertEquals(2, satisfaction.ratedMemberCount)
        assertEquals(4, satisfaction.memberCount)
        // Two HAPPY (100 each) averages to 100 — the two silent members must not drag it to 50.
        assertEquals(100, satisfaction.averageScore)
    }

    @Test
    fun `mixed ratings average onto the shared 0-100 scale`() {
        val members = listOf(
            member("a", WeeklySatisfaction.BAD, THIS_WEEK),
            member("b", WeeklySatisfaction.NEUTRAL, THIS_WEEK),
            member("c", WeeklySatisfaction.HAPPY, THIS_WEEK)
        )

        val satisfaction = CommunitySatisfaction.from(members, THIS_WEEK)

        assertEquals(3, satisfaction.ratedMemberCount)
        assertEquals(50, satisfaction.averageScore)
    }

    @Test
    fun `a rating left over from a previous week is ignored rather than carried forward`() {
        val members = listOf(
            member("a", WeeklySatisfaction.HAPPY, LAST_WEEK),
            member("b", WeeklySatisfaction.BAD, THIS_WEEK)
        )

        val satisfaction = CommunitySatisfaction.from(members, THIS_WEEK)

        assertEquals(1, satisfaction.ratedMemberCount)
        assertEquals(2, satisfaction.memberCount)
        assertEquals(WeeklySatisfaction.BAD.score, satisfaction.averageScore)
    }

    @Test
    fun `a whole circle whose ratings are all stale reads as unrated`() {
        val members = listOf(
            member("a", WeeklySatisfaction.HAPPY, LAST_WEEK),
            member("b", WeeklySatisfaction.NEUTRAL, LAST_WEEK)
        )

        val satisfaction = CommunitySatisfaction.from(members, THIS_WEEK)

        assertFalse(satisfaction.hasRatings)
        assertEquals(0, satisfaction.ratedMemberCount)
    }

    @Test
    fun `a rating with no week stamp at all never counts`() {
        val members = listOf(member("a", WeeklySatisfaction.HAPPY, weekStart = null))

        val satisfaction = CommunitySatisfaction.from(members, THIS_WEEK)

        assertFalse(satisfaction.hasRatings)
    }

    @Test
    fun `an all-negative week scores zero rather than reading as unrated`() {
        val members = listOf(
            member("a", WeeklySatisfaction.BAD, THIS_WEEK),
            member("b", WeeklySatisfaction.BAD, THIS_WEEK)
        )

        val satisfaction = CommunitySatisfaction.from(members, THIS_WEEK)

        assertTrue(satisfaction.hasRatings)
        assertEquals(0, satisfaction.averageScore)
    }
}
