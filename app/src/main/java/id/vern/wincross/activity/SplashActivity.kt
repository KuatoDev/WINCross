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
import id.vern.wincross.managers.*
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {
  companion object {
    private const val REQUEST_CODE = 1001
    private const val TAG = "SplashActivity"
  }
  private lateinit var sharedPreferences: SharedPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    sharedPreferences = getSharedPreferences(ThemeManager.PREFS_NAME, Context.MODE_PRIVATE)
    ThemeManager(this).initializeTheme(this)
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

  private fun requestPermissionsIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.requestPermissions(
        this, arrayOf(android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC), REQUEST_CODE)
    }
  }

  private suspend fun initializeApp() {
    checkAndGetDeviceModel(this)
    val prefs = getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    // Get or set default Windows path
    val mountPath = prefs.getString("Windows Mount Path", null)
    val windowsPath = when {
      // If Windows Mount Path has a value, use it
      !mountPath.isNullOrEmpty() -> mountPath
      // If null or empty, set default path and save it
      else -> "${Environment.getExternalStorageDirectory().path}/WINCross/Windows".also {
        defaultPath ->
        prefs.edit()
        .putString("Windows Mount Path", defaultPath)
        .apply()
        Log.d(TAG, "Setting default Windows path: $defaultPath")
      }
    }

    // Setup backup path
    val backupPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Backup"

    Log.d(TAG, "Backup folder path: $backupPath")
    Log.d(TAG, "Windows folder path: $windowsPath")

    // Initialize required directories and configurations
    withContext(Dispatchers.IO) {
      // Create required directories
      UtilityHelper.createFolderIfNotExists(listOf(backupPath, windowsPath))

      // Copy assets and check partitions
      launch {
        AssetsManager.copyAssetsToExecutableDir(this@SplashActivity)
      }
      launch {
        GetPartitions.checkAndSaveActiveSlot(this@SplashActivity)
      }
      launch {
        GetPartitions.checkAndSaveAllPartitionsIfNeeded(this@SplashActivity)
      }
    }
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