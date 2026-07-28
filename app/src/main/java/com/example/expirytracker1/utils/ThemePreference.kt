package com.example.expirytracker1.utils

import android.content.Context
import com.example.expirytracker1.ui.theme.ThemeMode

class ThemePreference(context: Context) {

    private val prefs =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    fun saveTheme(theme: ThemeMode) {
        prefs.edit().putString("theme_mode", theme.name).apply()
    }

    fun getTheme(): ThemeMode {
        val value = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return ThemeMode.valueOf(value!!)
    }
}