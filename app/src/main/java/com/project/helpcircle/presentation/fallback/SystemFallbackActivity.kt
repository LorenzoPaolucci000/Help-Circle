package com.project.helpcircle.presentation.fallback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.helpcircle.ui.theme.HelpCircleTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * A standalone, dialog-themed activity the accessibility service launches on top of whatever app
 * the user is in: the System Fallback prompt, shown autonomously when the community is offline or
 * hasn't responded to a crisis in time. Launched from a non-Activity context with
 * FLAG_ACTIVITY_NEW_TASK; being a small floating dialog window (see Theme.HelpCircle.Fallback)
 * rather than a SYSTEM_ALERT_WINDOW overlay sidesteps that permission's risk entirely, since this
 * prompt needs to be tappable and the overlay controllers only ever draw non-interactive views.
 */
@AndroidEntryPoint
class SystemFallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelpCircleTheme {
                SystemFallbackScreen(
                    onDismissed = { finish() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, SystemFallbackActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
    }
}
