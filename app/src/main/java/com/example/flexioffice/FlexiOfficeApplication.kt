package com.example.flexioffice

import android.app.Application
import com.example.flexioffice.fcm.FCMTokenManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FlexiOfficeApplication : Application() {
    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    override fun onCreate() {
        super.onCreate()
        // Initialize FCM token management
        fcmTokenManager.initializeFCM()
    }
}
