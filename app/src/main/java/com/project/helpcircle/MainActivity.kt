package com.project.helpcircle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.project.helpcircle.presentation.navigation.HelpCircleNavHost
import com.project.helpcircle.presentation.navigation.TabDestination
import com.project.helpcircle.ui.theme.HelpCircleTheme
import dagger.hilt.android.AndroidEntryPoint

/** App entry point; hosts the Compose UI. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialTab = initialTabFromIntent()
        setContent {
            HelpCircleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HelpCircleNavHost(
                        modifier = Modifier.padding(innerPadding),
                        initialTab = initialTab
                    )
                }
            }
        }
    }

    /**
     * Which tab a notification asked for, if this launch came from one. Only a hint: a user who
     * hasn't finished onboarding is still routed through it, since a tab they cannot reach yet is
     * no use to them.
     */
    private fun initialTabFromIntent(): TabDestination? = when (intent?.getStringExtra(EXTRA_INITIAL_TAB)) {
        TAB_HELP -> TabDestination.HELP
        else -> null
    }

    companion object {
        const val EXTRA_INITIAL_TAB = "com.project.helpcircle.INITIAL_TAB"
        const val TAB_HELP = "help"
    }
}
