package com.project.helpcircle.data.repository

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.TextNudgeStyle
import com.project.helpcircle.domain.repository.NudgeRepository
import com.project.helpcircle.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [NudgeRepository]. Nudges ride the same `/communities/{id}/events`
 * collection [CommunityRepositoryImpl] writes crisis alerts to, tagged `type = "nudge_sent"`
 * plus [FIELD_TARGET_ID] so a specific recipient's listener can pick them out, and either
 * [FIELD_STYLE] or [FIELD_LEVEL] depending on [Nudge] subtype. None of this is on the Zero-PII
 * blacklist (app names, scroll data, usage durations, IA_ind, usage timestamps) — it's
 * sender-authored nudge content the recipient's device needs to actually render the
 * intervention, not passively-collected behavioral data. [FIELD_STYLE] carries only the
 * [TextNudgeStyle] preset name, never free text, so a recipient's device renders the message
 * from its own local copy of [TextNudgeStyle.message] rather than trusting sender-supplied text.
 *
 * [incomingNudges] runs one `collectionGroup` listener across every community's `events`
 * subcollection, filtered to `nudge_sent` docs addressed to the local anonymous UID. It only
 * emits documents added after the listener attaches, so past interventions aren't replayed on
 * every app start. This query needs a Firestore collection-group index on
 * (`targetId` ASC, `type` ASC) over `events`, configured in the Firebase console.
 */
class NudgeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : NudgeRepository {

    override val incomingNudges: Flow<Nudge> = callbackFlow {
        val recipientId = userRepository.getOrCreateIdentity().anonymousHash
        var isFirstSnapshot = true
        val registration = firestore.collectionGroup(EVENTS_COLLECTION)
            .whereEqualTo(FIELD_TARGET_ID, recipientId)
            .whereEqualTo(FIELD_TYPE, EVENT_TYPE_NUDGE_SENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (isFirstSnapshot) {
                    isFirstSnapshot = false
                    return@addSnapshotListener
                }
                snapshot?.documentChanges
                    ?.filter { it.type == DocumentChange.Type.ADDED }
                    ?.forEach { change ->
                        val doc = change.document
                        doc.getString(FIELD_NUDGE_TYPE)?.let { toNudge(it, doc) }?.let { trySend(it) }
                    }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendNudge(communityId: String, targetUserId: String, nudge: Nudge) {
        val senderId = userRepository.getOrCreateIdentity().anonymousHash
        val payload = buildMap {
            put(FIELD_TYPE, EVENT_TYPE_NUDGE_SENT)
            put(FIELD_COMMUNITY_ID, communityId)
            put(FIELD_SENDER_ID, senderId)
            put(FIELD_TARGET_ID, targetUserId)
            put(FIELD_TIMESTAMP, FieldValue.serverTimestamp())
            putAll(nudge.toEventFields())
        }
        firestore.collection(COMMUNITIES_COLLECTION).document(communityId)
            .collection(EVENTS_COLLECTION)
            .add(payload)
            .await()
    }

    private fun Nudge.toEventFields(): Map<String, Any?> = when (this) {
        is Nudge.Text -> mapOf(FIELD_NUDGE_TYPE to NUDGE_TYPE_TEXT, FIELD_STYLE to style.name)
        is Nudge.GreyscaleLevel -> mapOf(FIELD_NUDGE_TYPE to NUDGE_TYPE_GREYSCALE, FIELD_LEVEL to level)
        Nudge.Haptic -> mapOf(FIELD_NUDGE_TYPE to NUDGE_TYPE_HAPTIC)
        Nudge.ContentBlur -> mapOf(FIELD_NUDGE_TYPE to NUDGE_TYPE_BLUR)
    }

    private fun toNudge(nudgeType: String, doc: DocumentSnapshot): Nudge? = when (nudgeType) {
        NUDGE_TYPE_TEXT -> doc.getString(FIELD_STYLE)
            ?.let { styleName -> runCatching { TextNudgeStyle.valueOf(styleName) }.getOrNull() }
            ?.let { Nudge.Text(it) }
        NUDGE_TYPE_GREYSCALE -> doc.getLong(FIELD_LEVEL)?.toInt()?.let { Nudge.GreyscaleLevel(it) }
        NUDGE_TYPE_HAPTIC -> Nudge.Haptic
        NUDGE_TYPE_BLUR -> Nudge.ContentBlur
        else -> null
    }

    companion object {
        private const val COMMUNITIES_COLLECTION = "communities"
        private const val EVENTS_COLLECTION = "events"
        private const val EVENT_TYPE_NUDGE_SENT = "nudge_sent"
        private const val FIELD_TYPE = "type"
        private const val FIELD_COMMUNITY_ID = "communityId"
        private const val FIELD_SENDER_ID = "senderId"
        private const val FIELD_TARGET_ID = "targetId"
        private const val FIELD_NUDGE_TYPE = "nudgeType"
        private const val FIELD_STYLE = "style"
        private const val FIELD_LEVEL = "level"
        private const val FIELD_TIMESTAMP = "timestamp"

        private const val NUDGE_TYPE_TEXT = "text"
        private const val NUDGE_TYPE_GREYSCALE = "grayscale"
        private const val NUDGE_TYPE_HAPTIC = "haptic"
        private const val NUDGE_TYPE_BLUR = "blur"
    }
}
