package com.shohan.sudokuai.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.shohan.sudokuai.R

object ThemeManager {

    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_THEME = "theme_dark"

    fun isDarkTheme(context: Context): Boolean {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_THEME, false)
    }

    fun setDarkTheme(context: Context, isDark: Boolean) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_THEME, isDark).apply()
    }

    fun applyTheme(activity: AppCompatActivity) {
        if (isDarkTheme(activity)) {
            activity.setTheme(R.style.Theme_SudokuAI_Dark)
        } else {
            activity.setTheme(R.style.Theme_SudokuAI_Light)
        }
    }

    fun toggleTheme(activity: AppCompatActivity) {
        val current = isDarkTheme(activity)
        setDarkTheme(activity, !current)
        activity.recreate()
    }
}
