package com.project.helpcircle.presentation.common

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.project.helpcircle.ui.theme.Elevation
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Spacing

/**
 * Whether the OS will actually display this app's notifications, re-checked whenever the app comes
 * back to the foreground so returning from system settings updates the UI without a restart, the
 * same live-update property [MonitoringStatusViewModel] gives the monitoring banner.
 *
 * Deliberately asks [NotificationManagerCompat.areNotificationsEnabled] rather than only checking
 * the POST_NOTIFICATIONS grant: notifications can also be switched off wholesale from system
 * settings, which the runtime permission check would report as fine.
 */
@Composable
fun rememberNotificationsEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

/**
 * Asks for POST_NOTIFICATIONS the first time this enters composition, on the versions that require
 * it. The permission was declared in the manifest from the start but never actually requested, so
 * on Android 13 and above every notification this app posts, including a peer's crisis alert, was
 * silently dropped.
 *
 * The system only shows its dialog while the request is still open; once the user has refused twice
 * it returns immediately and [NotificationsDisabledBanner] becomes the way back.
 */
@Composable
fun RequestNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Shown when notifications are switched off. Same reasoning as [MonitoringDisabledBanner]: with
 * notifications blocked a peer's crisis alert never reaches this device and nothing else about the
 * app looks wrong, so the failure would be completely invisible. Kept as a separate banner rather
 * than folded into that one because the cause and the fix are different, and a user can easily have
 * one problem without the other.
 */
@Composable
fun NotificationsDisabledBanner(
    onEnableClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Notifications are off",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "You won't hear about it when someone in your circle needs support, " +
                    "so nobody can count on you to notice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            Button(
                onClick = onEnableClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Turn notifications on")
            }
        }
    }
}
