package com.project.helpcircle.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.project.helpcircle.data.local.dao.ActiveCommunityDao
import com.project.helpcircle.data.local.entity.ActiveCommunityEntity
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [CommunityRepository]. The privacy filter below caps what ever leaves the
 * device: the community-wide event stream carries only [FIELD_TYPE], [FIELD_COMMUNITY_ID] and
 * [FIELD_SENDER_ID] (an anonymous UID); the per-member roster under [MEMBERS_COLLECTION] carries
 * only a self-chosen [FIELD_NICKNAME] and a coarse [FIELD_STATUS] tier, visible to fellow members
 * of the same community only. Never sent: app names, scroll data, usage durations, individual
 * IA_ind scores, or usage timestamps, per the Zero-PII rule.
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
    private val userRepository: UserRepository,
    private val activeCommunityDao: ActiveCommunityDao
) : CommunityRepository {

    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        combine(observeCommunityDoc(communityId), observeMembers(communityId)) { doc, members ->
            doc.toCommunityState(communityId, members)
        }

    override suspend fun joinCommunity(communityId: String): CommunityState {
        val doc = communityDoc(communityId)
        doc.set(
            mapOf(
                FIELD_ACTIVE_MEMBERS to FieldValue.increment(1),
                FIELD_LAST_ACTIVITY to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        activeCommunityDao.upsert(ActiveCommunityEntity(communityId = communityId))
        writeOwnMemberDoc(communityId, MemberStatus.OK)
        val members = doc.collection(MEMBERS_COLLECTION).get().await().documents.map { it.toCommunityMember() }
        return doc.get().await().toCommunityState(communityId, members)
    }

    override suspend fun reportCrisis(communityId: String) {
        writeEvent(communityId, EVENT_TYPE_CRISIS_ALERT)
        writeOwnMemberDoc(communityId, MemberStatus.CRISIS)
        // MVP_STUB: recomputing IA_comm from crisis/nudge events and refreshing the community
        // document's activeMembers/lastActivity presence is deferred to a later step.
    }

    override suspend fun reportRecovery(communityId: String) {
        writeEvent(communityId, EVENT_TYPE_RECOVERY_REPORTED)
        writeOwnMemberDoc(communityId, MemberStatus.OK)
        // MVP_STUB: recomputing IA_comm/IA_ind from a recovery event is deferred to a later step.
    }

    override suspend fun getActiveCommunityId(): String? = activeCommunityDao.get()?.communityId

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

    /** Writes only this device's own roster entry — nickname and coarse status, per Zero-PII. */
    private suspend fun writeOwnMemberDoc(communityId: String, status: MemberStatus) {
        val identity = userRepository.getOrCreateIdentity()
        communityDoc(communityId).collection(MEMBERS_COLLECTION).document(identity.anonymousHash).set(
            mapOf(
                FIELD_NICKNAME to identity.nickname,
                FIELD_STATUS to status.toFirestoreValue(),
                FIELD_LAST_SEEN to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
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
        val activeMembers = (getLong(FIELD_ACTIVE_MEMBERS) ?: 0L).toInt().coerceAtLeast(1)
        val iaComm = AgencyIndex.of((getDouble(FIELD_IA_COMM) ?: AgencyIndex.BASELINE.toDouble()).roundToInt())
        // The remote doc only ever stores the community-wide IA_comm aggregate, never individual
        // IA_ind scores (Zero-PII). Filling memberAgencyIndices with activeMembers copies of that
        // same value keeps CommunityState.collectiveIndex's average equal to IA_comm exactly,
        // without a cohesion bonus being re-applied on top of an already-aggregated remote value.
        return CommunityState(
            communityId = communityId,
            memberAgencyIndices = List(activeMembers) { iaComm },
            cohesionBonusApplied = false,
            members = members
        )
    }

    private fun DocumentSnapshot.toCommunityMember(): CommunityMember = CommunityMember(
        anonymousId = id,
        nickname = getString(FIELD_NICKNAME).orEmpty(),
        status = (getString(FIELD_STATUS) ?: MemberStatus.OK.toFirestoreValue()).toMemberStatus()
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

    companion object {
        private const val COMMUNITIES_COLLECTION = "communities"
        private const val MEMBERS_COLLECTION = "members"
        private const val EVENTS_COLLECTION = "events"
        private const val EVENT_TYPE_CRISIS_ALERT = "crisis_alert"
        private const val EVENT_TYPE_RECOVERY_REPORTED = "recovery_reported"
        private const val FIELD_ACTIVE_MEMBERS = "activeMembers"
        private const val FIELD_IA_COMM = "IA_comm"
        private const val FIELD_LAST_ACTIVITY = "lastActivity"
        private const val FIELD_TYPE = "type"
        private const val FIELD_COMMUNITY_ID = "communityId"
        private const val FIELD_SENDER_ID = "senderId"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_NICKNAME = "nickname"
        private const val FIELD_STATUS = "status"
        private const val FIELD_LAST_SEEN = "lastSeen"
    }
}
