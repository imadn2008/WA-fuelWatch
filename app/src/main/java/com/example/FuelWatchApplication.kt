package com.example

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

class FuelWatchApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        setupGlobalExceptionHandler()
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("FuelWatchCrash", "Uncaught exception in thread ${thread.name}", exception)

            // Log structured crash data here or send to Crashlytics

            // Delegate to the default handler to prevent the app from freezing
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}
