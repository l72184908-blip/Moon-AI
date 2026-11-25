package com.moon.ai

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/** Locks the app to the pure-black theme (no light mode, ever). */
class MoonAiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
