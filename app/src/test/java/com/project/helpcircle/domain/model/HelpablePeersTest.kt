package com.project.helpcircle.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun member(id: String, status: MemberStatus) =
    CommunityMember(anonymousId = id, nickname = "nick-$id", status = status, agencyScore = 50)

class HelpablePeersTest {

    @Test
    fun `keeps only at-risk and crisis peers`() {
        val members = listOf(
            member("self", MemberStatus.OK),
            member("calm", MemberStatus.OK),
            member("slipping", MemberStatus.AT_RISK),
            member("struggling", MemberStatus.CRISIS)
        )

        val helpable = HelpablePeers.from(members, selfAnonymousId = "self")

        assertEquals(listOf("struggling", "slipping"), helpable.peers.map { it.anonymousId })
    }

    @Test
    fun `counts every peer regardless of status`() {
        val members = listOf(
            member("self", MemberStatus.OK),
            member("calm", MemberStatus.OK),
            member("slipping", MemberStatus.AT_RISK)
        )

        val helpable = HelpablePeers.from(members, selfAnonymousId = "self")

        // Two peers exist even though only one of them can be helped right now — the distinction the
        // Help screen needs to tell "no peers yet" from "your peers are doing fine".
        assertEquals(2, helpable.totalPeerCount)
        assertEquals(1, helpable.peers.size)
    }

    @Test
    fun `excludes this device's own entry even while it is in crisis`() {
        val members = listOf(
            member("self", MemberStatus.CRISIS),
            member("peer", MemberStatus.AT_RISK)
        )

        val helpable = HelpablePeers.from(members, selfAnonymousId = "self")

        assertEquals(listOf("peer"), helpable.peers.map { it.anonymousId })
        assertEquals(1, helpable.totalPeerCount)
    }

    @Test
    fun `lists peers in crisis before peers merely at risk`() {
        val members = listOf(
            member("self", MemberStatus.OK),
            member("at-risk-first", MemberStatus.AT_RISK),
            member("crisis-second", MemberStatus.CRISIS),
            member("at-risk-third", MemberStatus.AT_RISK),
            member("crisis-fourth", MemberStatus.CRISIS)
        )

        val helpable = HelpablePeers.from(members, selfAnonymousId = "self")

        assertEquals(
            listOf("crisis-second", "crisis-fourth", "at-risk-first", "at-risk-third"),
            helpable.peers.map { it.anonymousId }
        )
    }

    @Test
    fun `reports no helpable peers when the whole circle is doing fine`() {
        val members = listOf(
            member("self", MemberStatus.OK),
            member("calm-one", MemberStatus.OK),
            member("calm-two", MemberStatus.OK)
        )

        val helpable = HelpablePeers.from(members, selfAnonymousId = "self")

        assertTrue(helpable.peers.isEmpty())
        assertEquals(2, helpable.totalPeerCount)
    }

    @Test
    fun `reports no peers at all for a circle of one`() {
        val helpable = HelpablePeers.from(listOf(member("self", MemberStatus.CRISIS)), selfAnonymousId = "self")

        assertTrue(helpable.peers.isEmpty())
        assertEquals(0, helpable.totalPeerCount)
    }
}
