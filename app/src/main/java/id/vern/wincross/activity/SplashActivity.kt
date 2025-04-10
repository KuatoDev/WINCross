package id.vern.wincross.activity

import android.content.*
import android.os.*
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.*
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.topjohnwu.superuser.Shell
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.*
import java.io.File
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {
  companion object {
    private const val REQUEST_CODE = 1001
    private const val TAG = "SplashActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    applyTheme()
    super.onCreate(savedInstanceState)
    setContentView(R.layout.splash_screen)
    Shell.getShell()
    if (Shell.isAppGrantedRoot()!= true) {
      Log.d(TAG, "Device is not rooted")
      DialogHelper.showPopupNotifications(this, "Give root access to continue...")
      return
    }

    requestPermissionsIfNeeded()
    lifecycleScope.launch(Dispatchers.IO) {
      initializeApp()

      withContext(Dispatchers.Main) {
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
      }
    }
  }

  private fun applyTheme() {
    val prefs = getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val themeColor = prefs.getString(getString(R.string.key_theme_color), "default")
    val theme = prefs.getString(getString(R.string.key_theme), "default")

    val mode =
    when (theme) {
      "dark" -> AppCompatDelegate.MODE_NIGHT_YES
      "light" -> AppCompatDelegate.MODE_NIGHT_NO
      else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mode)

    val themeId =
    when (themeColor) {
      "blue" -> R.style.Theme_MyApp_Blue
      "red" -> R.style.Theme_MyApp_Red
      "green" -> R.style.Theme_MyApp_Green
      "yellow" -> R.style.Theme_MyApp_Yellow
      else -> R.style.Theme_MyApp_Default
    }
    setTheme(themeId)
  }

  private fun requestPermissionsIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.requestPermissions(
        this, arrayOf(android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC), REQUEST_CODE)
    }
  }

  private suspend fun initializeApp() {
    checkAndGetDeviceModel(this)
    val backupPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Backup"
    val windowsFolderPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"

    Log.d(TAG, "Backup folder path: $backupPath")
    Log.d(TAG, "Windows folder path: $windowsFolderPath")

    UtilityHelper.createFolderIfNotExists(listOf(backupPath, windowsFolderPath))
    AssetsManager.copyAssetsToExecutableDir(this)
    GetPartitions.checkAndSaveActiveSlot(this)
    GetPartitions.checkAndSaveAllPartitionsIfNeeded(this)
  }

  fun checkAndGetDeviceModel(context: Context) {
    val preferences = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val deviceModel = preferences.getString("device_model", null)
    if (deviceModel.isNullOrEmpty()) {
      val newDeviceModel = Utils.getDeviceModel(context)
      preferences.edit().apply {
        putString("device_model", newDeviceModel)
        apply()
      }
    }
  }
}