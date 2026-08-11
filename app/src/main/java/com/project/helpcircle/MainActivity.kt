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
import com.project.helpcircle.ui.theme.HelpCircleTheme
import dagger.hilt.android.AndroidEntryPoint

/** App entry point; hosts the Compose UI. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelpCircleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HelpCircleNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
