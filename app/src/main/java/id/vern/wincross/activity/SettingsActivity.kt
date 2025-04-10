package id.vern.wincross.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import id.vern.wincross.databinding.ActivitySettingsBinding
import androidx.preference.PreferenceManager
import android.content.*
import id.vern.wincross.R
import id.vern.wincross.fragments.*
import android.view.MenuItem

class SettingsActivity : AppCompatActivity() {
  private lateinit var binding: ActivitySettingsBinding
  private lateinit var prefs: SharedPreferences
  companion object {
    private const val TAG = "SettingsActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    prefs = getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)

    val themeColor = prefs.getString(getString(R.string.key_theme_color), "default")
    val themeId = when (themeColor) {
      "blue" -> R.style.Theme_MyApp_Blue
      "red" -> R.style.Theme_MyApp_Red
      "green" -> R.style.Theme_MyApp_Green
      "yellow" -> R.style.Theme_MyApp_Yellow
      else -> R.style.Theme_MyApp_Default
    }
    setTheme(themeId)
    binding = ActivitySettingsBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setupToolbar()
    Log.d(TAG, "onCreate: Toolbar title set to 'WIN Cross'")

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
      .replace(R.id.fragment_container, SettingsFragment())
      .commit()
    }
  }
  private fun setupToolbar() {
    binding.toolbarlayout.toolbar.apply {
      title = getString(R.string.settings_title)
      setSupportActionBar(this)
      setNavigationIcon(R.drawable.ic_cog)
    }
  }
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}