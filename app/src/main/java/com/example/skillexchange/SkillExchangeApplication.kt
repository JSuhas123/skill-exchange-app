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
        
        // Set global exception handler to prevent crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SkillExchange", "Uncaught exception in thread: ${thread.name}", throwable)
            // Let the default handler also process it
            kotlin.system.exitProcess(1)
        }
        
        try {
            // Initialize logging with error handling
            ProductionLogger.init(isDebug = BuildConfig.DEBUG)
        } catch (e: Exception) {
            android.util.Log.e("SkillExchange", "Failed to initialize logging", e)
        }
        
        try {
            // Enable Firebase Analytics
            Firebase.analytics.setAnalyticsCollectionEnabled(true)
        } catch (e: Exception) {
            android.util.Log.e("SkillExchange", "Failed to enable Firebase Analytics", e)
        }
        
        try {
            // Enable Crashlytics
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
        } catch (e: Exception) {
            android.util.Log.e("SkillExchange", "Failed to enable Firebase Crashlytics", e)
        }
    }
}
