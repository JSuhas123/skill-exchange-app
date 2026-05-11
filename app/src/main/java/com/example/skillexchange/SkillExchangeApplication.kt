package com.example.skillexchange

import android.app.Application
import com.example.skillexchange.utils.ProductionLogger
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SkillExchangeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        ProductionLogger.init(isDebug = BuildConfig.DEBUG)
        
        // Enable Firebase Analytics
        Firebase.analytics.setAnalyticsCollectionEnabled(true)
        
        // Enable Crashlytics
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
    }
}
