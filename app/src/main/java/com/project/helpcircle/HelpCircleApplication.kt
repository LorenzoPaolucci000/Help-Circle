package com.project.helpcircle

import android.app.Application
import com.project.helpcircle.os.HelpCircleNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Application entry point that bootstraps the Hilt dependency graph. */
@HiltAndroidApp
class HelpCircleApplication : Application() {

    @Inject
    lateinit var notificationManager: HelpCircleNotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager.createNotificationChannels()
    }
}
