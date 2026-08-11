package com.project.helpcircle.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [CommunityRepository]. Every outgoing write goes through the privacy filter
 * below: only [FIELD_TYPE], [FIELD_COMMUNITY_ID] and [FIELD_SENDER_ID] (an anonymous UID) ever
 * leave the device — never app names, scroll data, usage durations, individual IA_ind scores,
 * or usage timestamps, per Zero-PII rule.
 *
 * [observeCommunityState] is a cold Firestore snapshot listener: it starts on subscription and
 * is torn down when the collecting coroutine is cancelled, so a lifecycle-aware collector (e.g.
 * a future ViewModel using `repeatOnLifecycle(STARTED)`) naturally suspends it while the app is
 * backgrounded, addressing Doze/battery constraint at the presentation layer.
 */
class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : CommunityRepository {

    override fun observeCommunityState(communityId: String): Flow<CommunityState> = callbackFlow {
        val registration = communityDoc(communityId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot!!.toCommunityState(communityId))
        }
        awaitClose { registration.remove() }
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
        return doc.get().await().toCommunityState(communityId)
    }

    override suspend fun reportCrisis(communityId: String) {
        val senderId = userRepository.getOrCreateIdentity().anonymousHash
        communityDoc(communityId).collection(EVENTS_COLLECTION).add(
            mapOf(
                FIELD_TYPE to EVENT_TYPE_CRISIS_ALERT,
                FIELD_COMMUNITY_ID to communityId,
                FIELD_SENDER_ID to senderId,
                FIELD_TIMESTAMP to FieldValue.serverTimestamp()
            )
        ).await()
        // MVP_STUB: recomputing IA_comm from crisis/nudge events and refreshing the community
        // document's activeMembers/lastActivity presence is deferred to a later step.
    }

    private fun communityDoc(communityId: String) =
        firestore.collection(COMMUNITIES_COLLECTION).document(communityId)

    private fun DocumentSnapshot.toCommunityState(communityId: String): CommunityState {
        val activeMembers = (getLong(FIELD_ACTIVE_MEMBERS) ?: 0L).toInt().coerceAtLeast(1)
        val iaComm = AgencyIndex.of((getDouble(FIELD_IA_COMM) ?: AgencyIndex.BASELINE.toDouble()).roundToInt())
        // The remote doc only ever stores the community-wide IA_comm aggregate, never individual
        // IA_ind scores (Zero-PII). Filling memberAgencyIndices with activeMembers copies of that
        // same value keeps CommunityState.collectiveIndex's average equal to IA_comm exactly,
        // without a cohesion bonus being re-applied on top of an already-aggregated remote value.
        return CommunityState(
            communityId = communityId,
            memberAgencyIndices = List(activeMembers) { iaComm },
            cohesionBonusApplied = false
        )
    }

    companion object {
        private const val COMMUNITIES_COLLECTION = "communities"
        private const val EVENTS_COLLECTION = "events"
        private const val EVENT_TYPE_CRISIS_ALERT = "crisis_alert"
        private const val FIELD_ACTIVE_MEMBERS = "activeMembers"
        private const val FIELD_IA_COMM = "IA_comm"
        private const val FIELD_LAST_ACTIVITY = "lastActivity"
        private const val FIELD_TYPE = "type"
        private const val FIELD_COMMUNITY_ID = "communityId"
        private const val FIELD_SENDER_ID = "senderId"
        private const val FIELD_TIMESTAMP = "timestamp"
    }
}
