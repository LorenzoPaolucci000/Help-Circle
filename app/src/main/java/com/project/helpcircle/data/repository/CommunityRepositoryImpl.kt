package com.project.helpcircle.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.project.helpcircle.data.local.dao.ActiveCommunityDao
import com.project.helpcircle.data.local.entity.ActiveCommunityEntity
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.CommunityWeeklyHistoryRepository
import com.project.helpcircle.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [CommunityRepository]. The privacy filter below caps what ever leaves the
 * device: the community-wide event stream carries only [FIELD_TYPE], [FIELD_COMMUNITY_ID] and
 * [FIELD_SENDER_ID] (an anonymous UID); the per-member roster under [MEMBERS_COLLECTION] carries
 * only a self-chosen [FIELD_NICKNAME], a coarse [FIELD_STATUS] tier, the derived
 * [FIELD_AGENCY_SCORE] (0-100), and the self-declared [FIELD_SATISFACTION] rating with the week it
 * describes ([FIELD_SATISFACTION_WEEK_START]), visible to fellow members of the same community
 * only. Never sent: app names, scroll data, usage durations, or usage timestamps, per the Zero-PII
 * rule — `agencyScore` is a derived index, not behavioral data, and reveals nothing about which
 * app, duration, or activity produced it, while `satisfaction` is an opinion the user typed in
 * themselves rather than anything observed about them.
 *
 * [observeCommunityState] is a cold Firestore snapshot listener: it starts on subscription and
 * is torn down when the collecting coroutine is cancelled, so a lifecycle-aware collector (e.g.
 * a future ViewModel using `repeatOnLifecycle(STARTED)`) naturally suspends it while the app is
 * backgrounded, addressing Doze/battery constraint at the presentation layer.
 *
 * Which community this device belongs to is tracked only locally, in [activeCommunityDao] — the
 * community ID itself isn't sensitive, but nothing about it needs to be readable from Firestore.
 */
class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val agencyRepository: AgencyRepository,
    private val activeCommunityDao: ActiveCommunityDao,
    private val communityWeeklyHistoryRepository: CommunityWeeklyHistoryRepository
) : CommunityRepository {

    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        combine(observeCommunityDoc(communityId), observeMembers(communityId)) { doc, members ->
            doc.toCommunityState(communityId, members)
        }.onEach { state ->
            // Lazy catch-up snapshot, same no-background-job pattern as the individual IA_ind's
            // own weekly reset: IA_comm is never archived server-side, so the first live update
            // this device happens to observe after a weekly boundary passes is what gets recorded
            // as that week's community snapshot.
            communityWeeklyHistoryRepository.ensureWeeklySnapshotApplied(communityId, state.collectiveIndex.value)
        }

    override suspend fun joinCommunity(communityId: String): CommunityState = retryingOnUnauthenticated {
        val identity = userRepository.getOrCreateIdentity()
        val agencyScore = agencyRepository.currentAgencyIndex.first().value
        val doc = communityDoc(communityId)
        val memberDoc = doc.collection(MEMBERS_COLLECTION).document(identity.anonymousHash)

        // A timed-out join isn't cancelled server-side — it can still land later — so a retry
        // could otherwise reach here a second time for the same physical join and double-count it
        // in activeMembers. Checking whether this device is already a member and bumping the
        // count atomically, in one transaction, makes that safe regardless of how many times this
        // ends up actually landing: the count only ever increments the first time.
        //
        // The same self-referential members-read rule described in leaveCommunity denies this
        // existence check with PERMISSION_DENIED whenever this device's own member doc doesn't
        // exist yet (e.g. rejoining a circle just left) — the opposite situation from
        // leaveCommunity's, but the same underlying cause. There, that denial means "already
        // gone"; here it means "not a member yet", so it's treated as alreadyMember = false and
        // the normal first-time-join path (increment + write) proceeds instead of aborting.
        firestore.runTransaction { transaction ->
            val alreadyMember = try {
                transaction.get(memberDoc).exists()
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) false else throw e
            }
            val communityUpdate = if (alreadyMember) {
                mapOf(FIELD_LAST_ACTIVITY to FieldValue.serverTimestamp())
            } else {
                mapOf(
                    FIELD_ACTIVE_MEMBERS to FieldValue.increment(1),
                    FIELD_LAST_ACTIVITY to FieldValue.serverTimestamp()
                )
            }
            transaction.set(doc, communityUpdate, SetOptions.merge())
            transaction.set(memberDoc, ownMemberFields(identity.nickname, MemberStatus.OK, agencyScore), SetOptions.merge())
        }.await()

        activeCommunityDao.upsert(ActiveCommunityEntity(communityId = communityId))
        val members = doc.collection(MEMBERS_COLLECTION).get().await().documents.map { it.toCommunityMember() }
        doc.get().await().toCommunityState(communityId, members)
    }

    override suspend fun createCommunity(
        communityId: String,
        inviteCode: String,
        name: String
    ): CommunityState =
        retryingOnUnauthenticated {
            val doc = communityDoc(communityId)
            doc.set(
                mapOf(
                    FIELD_INVITE_CODE to inviteCode,
                    // Written only here: the security rules leave name out of the allowed update
                    // keys, so this is the one moment in a circle's life it can be set.
                    FIELD_NAME to name,
                    // A literal 1, not FieldValue.increment(1): this document doesn't exist yet
                    // when a create is first attempted, and — since communityId is now stable
                    // across retries of the same attempt (see the interface doc) — this write may
                    // legitimately land more than once for the same logical create. An increment
                    // would double-count; a literal overwrite is naturally idempotent.
                    FIELD_ACTIVE_MEMBERS to 1,
                    FIELD_LAST_ACTIVITY to FieldValue.serverTimestamp()
                )
            ).await()
            activeCommunityDao.upsert(ActiveCommunityEntity(communityId = communityId))
            // No need to re-fetch the member roster or the community doc afterward — a freshly
            // created circle has exactly one member (the caller, just written below) and the invite
            // code and name are the ones we just wrote, so all three are already known locally.
            // This cuts what used to be 4 sequential network round-trips down to 2.
            val ownMember = writeOwnMemberDoc(communityId, MemberStatus.OK)
            CommunityState(
                communityId = communityId,
                memberAgencyIndices = listOf(AgencyIndex.of(ownMember.agencyScore)),
                cohesionBonusApplied = ownMember.agencyScore >= COHESION_THRESHOLD,
                members = listOf(ownMember),
                inviteCode = inviteCode,
                name = name
            )
        }

    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = retryingOnUnauthenticated {
        val match = firestore.collection(COMMUNITIES_COLLECTION)
            .whereEqualTo(FIELD_INVITE_CODE, inviteCode)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull() ?: return@retryingOnUnauthenticated null
        joinCommunity(match.id)
    }

    override suspend fun reportCrisis(communityId: String): Unit = retryingOnUnauthenticated {
        writeEvent(communityId, EVENT_TYPE_CRISIS_ALERT)
        writeOwnMemberDoc(communityId, MemberStatus.CRISIS)
    }

    override suspend fun reportRecovery(communityId: String): Unit = retryingOnUnauthenticated {
        writeEvent(communityId, EVENT_TYPE_RECOVERY_REPORTED)
        writeOwnMemberDoc(communityId, MemberStatus.OK)
    }

    /**
     * Writes only the tier and the derived score, as a merge of those two fields alone, so it can't
     * clobber the nickname the join path owns. Runs on every tier change during a scroll session,
     * which is why it deliberately does *not* also append an event the way [reportCrisis] does —
     * one document write per transition, nothing more.
     */
    override suspend fun publishStatus(communityId: String, status: MemberStatus): Unit =
        retryingOnUnauthenticated {
            val identity = userRepository.getOrCreateIdentity()
            val agencyScore = agencyRepository.currentAgencyIndex.first().value
            communityDoc(communityId).collection(MEMBERS_COLLECTION).document(identity.anonymousHash)
                .set(
                    mapOf(
                        FIELD_STATUS to status.toFirestoreValue(),
                        FIELD_AGENCY_SCORE to agencyScore,
                        FIELD_LAST_SEEN to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
        }

    override suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    ): Unit = retryingOnUnauthenticated {
        val identity = userRepository.getOrCreateIdentity()
        // A merge write of just these two fields: nickname/status/agencyScore are owned by
        // writeOwnMemberDoc and must not be clobbered with stale values from here.
        communityDoc(communityId).collection(MEMBERS_COLLECTION).document(identity.anonymousHash)
            .set(
                mapOf(
                    FIELD_SATISFACTION to satisfaction.toFirestoreValue(),
                    FIELD_SATISFACTION_WEEK_START to weekStartEpochMillis
                ),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun leaveCommunity(communityId: String): Unit = retryingOnUnauthenticated {
        val identity = userRepository.getOrCreateIdentity()
        val doc = communityDoc(communityId)
        val memberDoc = doc.collection(MEMBERS_COLLECTION).document(identity.anonymousHash)

        // Symmetric to joinCommunity's fix: a timed-out leave isn't cancelled server-side either,
        // so a retry could otherwise land twice for the same physical leave and double-decrement
        // activeMembers. Only delete-and-decrement if this device's member doc still exists; a
        // retry that finds it already gone (the first attempt's delete already landed) no-ops.
        //
        // The members subcollection's read rule only allows reading a doc that still exists (it
        // gates on that same doc's own existence), so once this device's delete has actually
        // landed server-side, a later retry's existence check above is itself denied rather than
        // cleanly reporting "not found" — Firestore surfaces that as PERMISSION_DENIED even though
        // auth and ownership are both fine. The delete rule itself has no existence precondition,
        // so that's the only realistic way this specific transaction can be denied; it's treated
        // the same as an already-completed leave rather than surfaced as a failure.
        try {
            firestore.runTransaction { transaction ->
                val stillMember = transaction.get(memberDoc).exists()
                if (stillMember) {
                    transaction.delete(memberDoc)
                    transaction.set(
                        doc,
                        mapOf(
                            FIELD_ACTIVE_MEMBERS to FieldValue.increment(-1),
                            FIELD_LAST_ACTIVITY to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                }
            }.await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
        }

        activeCommunityDao.clear()
    }

    override suspend fun getActiveCommunityId(): String? = activeCommunityDao.get()?.communityId

    override suspend fun getMemberCount(communityId: String): Int = retryingOnUnauthenticated {
        communityDoc(communityId).collection(MEMBERS_COLLECTION).get().await().size()
    }

    private fun observeCommunityDoc(communityId: String): Flow<DocumentSnapshot> = callbackFlow {
        val registration = communityDoc(communityId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot!!)
        }
        awaitClose { registration.remove() }
    }

    private fun observeMembers(communityId: String): Flow<List<CommunityMember>> = callbackFlow {
        val registration = communityDoc(communityId).collection(MEMBERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot!!.documents.map { it.toCommunityMember() })
            }
        awaitClose { registration.remove() }
    }

    /**
     * Writes only this device's own roster entry — nickname, coarse status, and derived
     * agencyScore, per Zero-PII — and hands back what it wrote, so callers that already know the
     * result (e.g. [createCommunity]) don't need a separate read to find out.
     */
    private suspend fun writeOwnMemberDoc(communityId: String, status: MemberStatus): CommunityMember {
        val identity = userRepository.getOrCreateIdentity()
        val agencyScore = agencyRepository.currentAgencyIndex.first().value
        communityDoc(communityId).collection(MEMBERS_COLLECTION).document(identity.anonymousHash)
            .set(ownMemberFields(identity.nickname, status, agencyScore), SetOptions.merge())
            .await()
        return CommunityMember(
            anonymousId = identity.anonymousHash,
            nickname = identity.nickname,
            status = status,
            agencyScore = agencyScore
        )
    }

    /** The field map [writeOwnMemberDoc] and [joinCommunity]'s transaction both write for this device's own roster entry. */
    private fun ownMemberFields(nickname: String, status: MemberStatus, agencyScore: Int): Map<String, Any> = mapOf(
        FIELD_NICKNAME to nickname,
        FIELD_STATUS to status.toFirestoreValue(),
        FIELD_AGENCY_SCORE to agencyScore,
        FIELD_LAST_SEEN to FieldValue.serverTimestamp()
    )

    /**
     * Retries [operation] exactly once, first triggering a fresh anonymous sign-in, if it fails
     * with an UNAUTHENTICATED Firestore error — the session's ID token can go stale (e.g. after a
     * long idle period), and a fresh sign-in resolves that transient condition without surfacing
     * an error to the caller for something that's expected to self-heal.
     */
    private suspend fun <T> retryingOnUnauthenticated(operation: suspend () -> T): T =
        try {
            operation()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                firebaseAuth.signInAnonymously().await()
                operation()
            } else {
                throw e
            }
        }

    private suspend fun writeEvent(communityId: String, type: String) {
        val senderId = userRepository.getOrCreateIdentity().anonymousHash
        communityDoc(communityId).collection(EVENTS_COLLECTION).add(
            mapOf(
                FIELD_TYPE to type,
                FIELD_COMMUNITY_ID to communityId,
                FIELD_SENDER_ID to senderId,
                FIELD_TIMESTAMP to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private fun communityDoc(communityId: String) =
        firestore.collection(COMMUNITIES_COLLECTION).document(communityId)

    private fun DocumentSnapshot.toCommunityState(
        communityId: String,
        members: List<CommunityMember>
    ): CommunityState {
        // IA_comm = Clamp(average(members' agencyScore) + B_cohesion, 0, 100); B_cohesion is +5
        // only if every member's agencyScore >= 60, else 0. CommunityState.collectiveIndex applies
        // this same average-plus-bonus-then-clamp formula, so it's enough to hand it each member's
        // real per-member index and whether the all-members->=-60 condition holds.
        return CommunityState(
            communityId = communityId,
            memberAgencyIndices = members.map { AgencyIndex.of(it.agencyScore) },
            cohesionBonusApplied = members.isNotEmpty() && members.all { it.agencyScore >= COHESION_THRESHOLD },
            members = members,
            inviteCode = getString(FIELD_INVITE_CODE).orEmpty(),
            // Blank for circles created before names existed; every screen showing it falls back to
            // the invite code, which was the only identifier those circles ever had.
            name = getString(FIELD_NAME).orEmpty()
        )
    }

    private fun DocumentSnapshot.toCommunityMember(): CommunityMember = CommunityMember(
        anonymousId = id,
        nickname = getString(FIELD_NICKNAME).orEmpty(),
        status = (getString(FIELD_STATUS) ?: MemberStatus.OK.toFirestoreValue()).toMemberStatus(),
        agencyScore = (getLong(FIELD_AGENCY_SCORE) ?: AgencyIndex.BASELINE.toLong()).toInt(),
        // Both stay null for a member who has never rated a week; the week stamp is carried through
        // as-is so the domain can tell a current-week rating from a leftover one.
        satisfaction = getString(FIELD_SATISFACTION)?.toWeeklySatisfaction(),
        satisfactionWeekStartEpochMillis = getLong(FIELD_SATISFACTION_WEEK_START)
    )

    private fun MemberStatus.toFirestoreValue(): String = when (this) {
        MemberStatus.OK -> "ok"
        MemberStatus.AT_RISK -> "at_risk"
        MemberStatus.CRISIS -> "crisis"
    }

    private fun String.toMemberStatus(): MemberStatus = when (this) {
        "at_risk" -> MemberStatus.AT_RISK
        "crisis" -> MemberStatus.CRISIS
        else -> MemberStatus.OK
    }

    private fun WeeklySatisfaction.toFirestoreValue(): String = when (this) {
        WeeklySatisfaction.BAD -> "bad"
        WeeklySatisfaction.NEUTRAL -> "neutral"
        WeeklySatisfaction.HAPPY -> "happy"
    }

    /** Null rather than a default for an unrecognised value: an unreadable rating must not be counted as a real one. */
    private fun String.toWeeklySatisfaction(): WeeklySatisfaction? = when (this) {
        "bad" -> WeeklySatisfaction.BAD
        "neutral" -> WeeklySatisfaction.NEUTRAL
        "happy" -> WeeklySatisfaction.HAPPY
        else -> null
    }

    companion object {
        private const val COMMUNITIES_COLLECTION = "communities"
        private const val MEMBERS_COLLECTION = "members"
        private const val EVENTS_COLLECTION = "events"
        private const val EVENT_TYPE_CRISIS_ALERT = "crisis_alert"
        private const val EVENT_TYPE_RECOVERY_REPORTED = "recovery_reported"
        private const val FIELD_ACTIVE_MEMBERS = "activeMembers"
        private const val FIELD_LAST_ACTIVITY = "lastActivity"
        private const val FIELD_TYPE = "type"
        private const val FIELD_COMMUNITY_ID = "communityId"
        private const val FIELD_SENDER_ID = "senderId"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_NICKNAME = "nickname"
        private const val FIELD_STATUS = "status"
        private const val FIELD_AGENCY_SCORE = "agencyScore"
        private const val FIELD_SATISFACTION = "satisfaction"
        private const val FIELD_SATISFACTION_WEEK_START = "satisfactionWeekStart"
        private const val FIELD_LAST_SEEN = "lastSeen"
        private const val FIELD_INVITE_CODE = "inviteCode"
        private const val FIELD_NAME = "name"

        /** Minimum per-member agencyScore, held by every member, for the IA_comm cohesion bonus. */
        private const val COHESION_THRESHOLD = 60
    }
}
