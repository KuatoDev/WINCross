package id.vern.wincross.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.*
import androidx.preference.PreferenceManager
import id.vern.wincross.databinding.ActivityToolboxBinding
import id.vern.wincross.helpers.*
import android.content.*
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import id.vern.wincross.R
import id.vern.wincross.operations.*
import android.view.MenuItem

class ToolboxActivity : AppCompatActivity() {
  private lateinit var binding: ActivityToolboxBinding
  private lateinit var sharedPreferences: SharedPreferences

  companion object {
    private const val TAG = "ToolboxActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val prefs = getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val themeColor = prefs.getString(getString(R.string.key_theme_color), "default")
    val theme = prefs.getString(getString(R.string.key_theme), "default")

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
    setTheme(themeId)

    binding = ActivityToolboxBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setupToolbar()

    binding.cvSTA.setOnClickListener {
      DialogHelper.showSTACreator(this, window.decorView.rootView) {
        lifecycleScope.launch {
          CreateSTA.RunSTAAsync(this@ToolboxActivity)
        }
      }
    }
    binding.cvARMSoftware.setOnClickListener {
      DialogHelper.showARMSoftware(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.Main) {
          ARMSoftware.extractARMSoftware(this@ToolboxActivity)
        }
      }
    }

    binding.cvScript.setOnClickListener {
      DialogHelper.showScriptToolbox(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.Main) {
          ScriptToolbox.extractScript(this@ToolboxActivity)
        }
      }
    }
    binding.cvDownloadFramework.setOnClickListener {
      DialogHelper.showDownloadFrameworks(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.Main) {
          FrameworkDownloader.downloadFrameworks(this@ToolboxActivity)
        }
      }
    }
    binding.cvReviAtlas.setOnClickListener {
      DialogHelper.showReviAtlas(this, window.decorView.rootView)
    }
  }
  private fun setupToolbar() {
    binding.toolbarlayout.toolbar.apply {
      title = getString(R.string.toolbox_title)
      setSupportActionBar(this)
      setNavigationIcon(R.drawable.ic_toolbox)
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