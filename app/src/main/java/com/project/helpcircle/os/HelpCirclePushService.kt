package com.project.helpcircle.os

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.project.helpcircle.R
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * Receives the circle's alerts and turns them into a notification.
 *
 * The messages are deliberately data-only rather than carrying a notification payload: that way
 * this runs for every message, including while the app is in the background, which is what makes
 * it possible to drop the sender's own copy. A topic reaches every subscriber, and the sender is
 * subscribed to their own circle, so without that filter a user in crisis would be told about
 * themselves.
 */
@AndroidEntryPoint
class HelpCirclePushService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var communityRepository: CommunityRepository

    @Inject
    lateinit var notificationManager: HelpCircleNotificationManager

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data[KEY_TYPE] != TYPE_PEER_CRISIS) return
        val senderId = data[KEY_SENDER_ID] ?: return

        // Blocking is acceptable and expected here: this callback already runs off the main thread
        // and the process may be torn down as soon as it returns, so work handed to a background
        // scope could simply never run.
        val ownId = runBlocking {
            runCatching { userRepository.getOrCreateIdentity().anonymousHash }.getOrNull()
        }
        if (ownId == null || ownId == senderId) {
            // Also drops the message when the identity can't be read at all. Staying silent is the
            // safer failure: telling someone their own crisis is worse than missing one alert.
            return
        }

        val nickname = data[KEY_NICKNAME]
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.peer_crisis_unknown_nickname)
        notificationManager.postPeerCrisisNotification(senderId, nickname)
    }

    /**
     * Topic subscriptions are tied to the messaging token, so a token this device has just been
     * issued carries none of them. Re-asserting here covers the refresh case directly, rather than
     * waiting for the next process start to notice.
     */
    override fun onNewToken(token: String) {
        runBlocking {
            runCatching { communityRepository.ensureAlertSubscription() }
                .onFailure { Log.w(TAG, "Could not re-subscribe after a token refresh", it) }
        }
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val KEY_SENDER_ID = "senderId"
        const val KEY_NICKNAME = "nickname"
        const val TYPE_PEER_CRISIS = "peer_crisis"
        const val TAG = "HelpCirclePushService"
    }
}
