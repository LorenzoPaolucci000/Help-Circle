package com.project.helpcircle.os

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.project.helpcircle.domain.repository.CommunityRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the "I'm back" action on a delivered-nudge notification: reverts any active visual
 * intervention, reports recovery to the device's active community, and dismisses the
 * notification. Uses `goAsync()` since a BroadcastReceiver's `onReceive` returns before the
 * suspend call to Firestore would finish. Reverting is unconditional and safe to call even when
 * no intervention is active — [GrayscaleInterventionController.revert] is a no-op in that case.
 */
@AndroidEntryPoint
class RecoveryActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var communityRepository: CommunityRepository

    @Inject
    lateinit var grayscaleInterventionController: GrayscaleInterventionController

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val pendingResult = goAsync()
        grayscaleInterventionController.revert()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                communityRepository.getActiveCommunityId()?.let { communityRepository.reportRecovery(it) }
                if (notificationId != -1) {
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
