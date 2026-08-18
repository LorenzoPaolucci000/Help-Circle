package com.project.helpcircle

import android.app.Application
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.os.HelpCircleNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Application entry point that bootstraps the Hilt dependency graph. */
@HiltAndroidApp
class HelpCircleApplication : Application() {

    @Inject
    lateinit var notificationManager: HelpCircleNotificationManager

    @Inject
    lateinit var communityRepository: CommunityRepository

    /** Swallows failures for the same reason the accessibility service's scope does: nothing started here is worth crashing the app over. */
    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> })

    override fun onCreate() {
        super.onCreate()
        notificationManager.createNotificationChannels()
        installFirestoreListenerCrashGuard()
        // Process start, not app launch: this also runs when the accessibility service brings the
        // process up on its own, so a user who never opens the app still stays reachable.
        applicationScope.launch { communityRepository.ensureAlertSubscription() }
    }

    /**
     * A Firestore snapshot listener's error callback can be delivered on the main thread slightly
     * after the code that registered it has already started tearing down — most commonly, leaving
     * a community revokes read access to its own members subcollection out from under a listener
     * that's still subscribed to it (e.g. the Community tab, kept alive in the background by the
     * bottom nav's multi-back-stack pattern while the user leaves via Settings). That failure is
     * thrown from inside the Firestore SDK's own internal listener-delivery code — not from within
     * any coroutine this app launched — so it never reaches any of this app's own `.catch {}`
     * operators or `CoroutineExceptionHandler`s, no matter which scope they're attached to; it can
     * only be intercepted here, as an uncaught exception. Every other Firestore-listener call site
     * in this app already treats a rejected/closed listener as an expected, recoverable condition
     * (see `CommunityRepositoryImpl`, `CommunityDashboardViewModel`) rather than a bug, so the same
     * tolerance applies here — anything else still crashes normally, so real bugs still surface.
     */
    private fun installFirestoreListenerCrashGuard() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is FirebaseFirestoreException) {
                Log.w(TAG, "Ignoring a Firestore listener failure delivered after teardown", throwable)
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private companion object {
        const val TAG = "HelpCircleApplication"
    }
}
