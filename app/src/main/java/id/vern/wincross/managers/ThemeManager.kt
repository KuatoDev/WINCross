package id.vern.wincross.managers

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import id.vern.wincross.R

class ThemeManager(private val context: Context) {
  companion object {
    const val PREFS_NAME = "WinCross_preferences"
  }

  private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun initializeTheme(activity: Activity) {
    val themeColor =
        sharedPreferences.getString(activity.getString(R.string.key_theme_color), "default")
    val theme = sharedPreferences.getString(activity.getString(R.string.key_theme), "default")

    val themeId =
        when (themeColor) {
          "blue" -> R.style.Theme_MyApp_Blue
          "red" -> R.style.Theme_MyApp_Red
          "green" -> R.style.Theme_MyApp_Green
          "yellow" -> R.style.Theme_MyApp_Yellow
          else -> R.style.Theme_MyApp_Default
        }

    val mode =
        when (theme) {
          "dark" -> AppCompatDelegate.MODE_NIGHT_YES
          "light" -> AppCompatDelegate.MODE_NIGHT_NO
          else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

    AppCompatDelegate.setDefaultNightMode(mode)
    activity.setTheme(themeId)
  }
}
