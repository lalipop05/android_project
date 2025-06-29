package com.lali.dnd.services

import android.content.Context
import androidx.core.content.edit

class PermissionTracker {
    companion object {
        private const val STORAGE_NAME = "permission_tracker"
        private const val KEY_BACKGROUND_COUNT = "background_denied_count"
        private const val KEY_FOREGROUND_COUNT = "foreground_denied_count"

        fun incrementBackgroundDenialCount(context: Context) {
            val prefs = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_BACKGROUND_COUNT, 0)
            prefs.edit { putInt(KEY_BACKGROUND_COUNT, count + 1) }
        }

        fun backgroundPermanentlyDenied(context: Context): Boolean {
            val prefs = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_BACKGROUND_COUNT, 0)
            return count >= 1
        }

        fun incrementForegroundDenialCount(context: Context) {
            val prefs = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_FOREGROUND_COUNT, 0)
            prefs.edit { putInt(KEY_FOREGROUND_COUNT, count + 1) }
        }

        fun foregroundPermanentlyDenied(context: Context): Boolean {
            val prefs = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
            val count = prefs.getInt(KEY_FOREGROUND_COUNT, 0)
            return count >= 1
        }
    }
}