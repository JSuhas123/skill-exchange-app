package com.example.skillexchange.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

object ProductionLogger {
    
    fun init(isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }
    
    private class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            crashlytics.setCustomKey("Log Level", getPriorityString(priority))
            if (tag != null) {
                crashlytics.setCustomKey("Tag", tag)
            }
            crashlytics.log(message)
            
            if (t != null && priority == Log.ERROR) {
                crashlytics.recordException(t)
            }
        }
        
        private fun getPriorityString(priority: Int): String {
            return when (priority) {
                Log.VERBOSE -> "VERBOSE"
                Log.DEBUG -> "DEBUG"
                Log.INFO -> "INFO"
                Log.WARN -> "WARN"
                Log.ERROR -> "ERROR"
                Log.ASSERT -> "ASSERT"
                else -> "UNKNOWN"
            }
        }
    }
}
