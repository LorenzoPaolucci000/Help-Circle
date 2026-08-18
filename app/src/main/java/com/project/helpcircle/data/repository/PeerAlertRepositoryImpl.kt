package com.project.helpcircle.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.project.helpcircle.di.IoDispatcher
import com.project.helpcircle.domain.repository.PeerAlertRepository
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Sends the crisis alert through the circle's alert service, authenticated with the caller's own
 * anonymous Firebase ID token.
 *
 * The service exists because push messages cannot be sent from a client: doing so needs OAuth2
 * service-account credentials, and shipping those in the app would let anyone who unpacked it push
 * to every device in the project. It is reached over plain HTTP rather than through a client
 * library, since one POST does not justify another networking dependency.
 *
 * Only the community id is sent. Who is in crisis and what they are called are both derived by the
 * service from the ID token and the roster, so this request carries nothing about the user that the
 * circle could not already see.
 */
class PeerAlertRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerAlertRepository {

    override suspend fun alertCircle(communityId: String) {
        withContext(ioDispatcher) {
            try {
                post(communityId, idToken())
            } catch (e: Exception) {
                // Swallowed on purpose, per this repository's interface contract: this runs on the
                // detection path, and an unreachable alert service must not stop the crisis being
                // detected, scored and recorded locally. The circle simply misses this alert.
                Log.w(TAG, "Could not alert the circle", e)
            }
        }
    }

    /** The caller's anonymous ID token, signing in first if this device has no session yet. */
    private suspend fun idToken(): String {
        val user = firebaseAuth.currentUser ?: firebaseAuth.signInAnonymously().await().user
        return requireNotNull(user?.getIdToken(false)?.await()?.token) { "no id token" }
    }

    private fun post(communityId: String, idToken: String) {
        val connection = (URL(ALERT_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("Authorization", "Bearer $idToken")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            val body = JSONObject().put("communityId", communityId).toString()
            connection.outputStream.use { it.write(body.toByteArray()) }
            val status = connection.responseCode
            if (status !in 200..299) {
                Log.w(TAG, "Alert service refused the request: $status")
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        /**
         * Public by design: the endpoint is useless without a valid Firebase ID token for a member
         * of the circle being alerted, both of which it verifies before sending anything.
         */
        const val ALERT_ENDPOINT = "https://helpcircle-alerts.helpcircle-unicam.workers.dev"

        /**
         * Shorter than the 15s the join/create/leave screens allow: those have a user waiting on a
         * result, whereas nothing here is blocked on the outcome, so a slow network should be given
         * up on quickly rather than holding a coroutine on the detection path open.
         */
        const val TIMEOUT_MILLIS = 5_000

        const val TAG = "PeerAlertRepository"
    }
}
