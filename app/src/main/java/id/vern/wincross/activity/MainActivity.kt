package id.vern.wincross.activity

import android.content.*
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import id.vern.wincross.R
import id.vern.wincross.databinding.ActivityMainBinding
import id.vern.wincross.handlers.*
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.*
import id.vern.wincross.managers.*
import kotlinx.coroutines.*
import id.vern.wincross.BuildConfig

class MainActivity : AppCompatActivity() {
  lateinit var binding: ActivityMainBinding
  lateinit var sharedPreferences: SharedPreferences
  lateinit var uefiUpdateManager: UefiUpdateManager
  lateinit var buttonManager: ButtonClickManager
  lateinit var uiStateManager: UIStateManager
  private lateinit var permissionManager: PermissionManager

  lateinit var mountManager: MountManager
  private lateinit var filePickerLauncher: ActivityResultLauncher<String>

  var isUefiAvailable = false
  var isWindowsInstalled = false
  var isWindowsMounted = false
  var isBackupExists = false
  private var isUefiDialogShowing = false


  private val switchHandler by lazy {
    SwitchHandler(this, sharedPreferences)
  }

  companion object {
    private const val TAG = "MainActivity"
    private const val PREFS_NAME = "WinCross_preferences"
    const val LAST_BACKUP_KEY = "last_backup_time"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initializeViews()
    initializeManagers()
    setupUI()
    checkInitialState()

    val currentVersion = BuildConfig.VERSION_NAME
    val updater = AppUpdaterManager(this)

    updater.checkForUpdates(currentVersion, object : AppUpdaterManager.UpdateCallback {
      override fun onUpdateAvailable(versionName: String, downloadUrl: String, releaseNotes: String) {
        DialogHelper.createDialog(
          context = this@MainActivity,
          rootView = window.decorView.rootView,
          title = getString(R.string.update_available),
          message = getString(R.string.new_version_info, versionName, releaseNotes),
          positiveButtonText = getString(R.string.download),
          negativeButtonText = getString(R.string.cancel),
          onPositive = {
            updater.downloadUpdate(downloadUrl)
          }
        )
      }

      override fun onNoUpdateAvailable() {
        Log.d(TAG, getString(R.string.already_latest_version))
      }

      override fun onError(error: String) {
        DialogHelper.createDialog(
          context = this@MainActivity,
          rootView = window.decorView.rootView,
          title = getString(R.string.update_check_failed),
          message = error,
          positiveButtonText = getString(R.string.yes),
          negativeButtonText = null
        )
      }
    })
  }

  private fun initializeViews() {
    sharedPreferences = getSharedPreferences(ThemeManager.PREFS_NAME, Context.MODE_PRIVATE)
    ThemeManager(this).initializeTheme(this)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
  }

  private fun initializeManagers() {
    mountManager = MountManager(this)
    buttonManager = ButtonClickManager(this)
    uefiUpdateManager = UefiUpdateManager(this)
    uiStateManager = UIStateManager(this, binding, sharedPreferences)
    permissionManager = PermissionManager(this)

    filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
      uri ->
      uri?.let {
        UEFIHelper.handleSelectedFile(it)
      }
    }

    UEFIHelper.initialize(this, filePickerLauncher)
  }

  private fun setupUI() {
    setupToolbar()
    setupButtonActions()
    setupSwitches()
  }

  private fun setupToolbar() {
    binding.toolbarlayout.toolbar.apply {
      title = getString(R.string.toolbar_title)
      setSupportActionBar(this)
      subtitle = "Cross-Platform Windows Control"
      setNavigationIcon(R.drawable.ic_toolbar)
    }
  }

  private fun setupButtonActions() {
    with(binding.mainbutton) {
      tvBackup.isSelected = true
      tvToolbox.isSelected = true
      tvMountWindows.isSelected = true
      tvSwitchToWindows.isSelected = true
      tvSelectUefi.isSelected = true
      tvRestore.isSelected = true

      btnBackup.setOnClickListener {
        buttonManager.handleBackup()
      }
      btnRestore.setOnClickListener {
        buttonManager.handleRestore()
      }
      btnSelectUEFI.setOnClickListener {
        buttonManager.handleSelectUEFI()
      }
      btnSwitchToWindows.setOnClickListener {
        buttonManager.handleSwitchToWindows()
      }
      btnToolbox.setOnClickListener {
        startActivity(Intent(this@MainActivity, ToolboxActivity::class.java))
      }
    }
  }

  private fun setupSwitches() {
    with(binding.mainpreferences) {
      msForceBackupToWin.apply {
        isChecked = sharedPreferences.getBoolean("force_backup_to_win", false)
        setOnCheckedChangeListener {
          _, isChecked ->
          switchHandler.handleBackupToWin(isChecked)
        }
      }

      msBackupBootIfEmpty.apply {
        isChecked = sharedPreferences.getBoolean("backup_boot_if_empty", false)
        setOnCheckedChangeListener {
          _, isChecked ->
          switchHandler.handleBackupBootIfEmpty(isChecked)
        }
      }

      msAutomaticMountWindows.apply {
        isChecked = sharedPreferences.getBoolean("automatic_mount_windows", false)
        setOnCheckedChangeListener {
          _, isChecked ->
          switchHandler.handleAutomaticMountWindows(isChecked)
        }
      }

      msMountToMnt.apply {
        isChecked = sharedPreferences.getString("Windows Mount Path", null) == "/mnt/Windows"
        setOnCheckedChangeListener {
          _, isChecked ->
          switchHandler.handleMountToMnt(isChecked)
        }
      }

      msFlashLogoWithUefi.apply {
        isChecked = sharedPreferences.getBoolean("flash_logo_with_uefi", false)
        visibility = if (shouldShowFlashLogo()) View.VISIBLE else View.GONE
        setOnCheckedChangeListener {
          _, isChecked ->
          switchHandler.handleFlashLogoWithUefi(isChecked)
        }
      }

      msAlwaysProvisionModem.apply {
        isChecked = sharedPreferences.getBoolean("always_provision_modem", false)
        setOnCheckedChangeListener {
          _, isChecked ->
          switchHandler.handleProvisionModem(isChecked)
        }
      }

      msUpdateUefi.apply {
        isChecked = sharedPreferences.getBoolean("uefi_auto_update", false)
        setOnCheckedChangeListener {
          _, isChecked ->
          switchHandler.handleUEFIAutoUpdate(isChecked)
        }
      }
    }
  }

  private fun shouldShowFlashLogo(): Boolean {
    val model = sharedPreferences.getString("device_model", "")?.lowercase()
    return model?.contains("vayu") == true || model?.contains("bhima") == true
  }

  private fun checkInitialState() {
    lifecycleScope.launch {
      checkFilesAndPermissions()
      updateUiBasedOnState()
    }
  }

  private suspend fun checkFilesAndPermissions() = withContext(Dispatchers.IO) {
    updateFileSystemState(true)
    withContext(Dispatchers.Main) {
      permissionManager.checkAndRequestAll()
    }
  }

  private suspend fun updateFileSystemState(showDialog: Boolean) {
    val selectedUefiPath = sharedPreferences.getString("UEFI", null)
    isUefiAvailable = if (selectedUefiPath.isNullOrEmpty()) {
      UtilityHelper.isUefiFileAvailable(this@MainActivity)
    } else {
      UtilityHelper.isFileExists(selectedUefiPath)
    }

    if (!isUefiAvailable && showDialog) {
      withContext(Dispatchers.Main) {
        showUefiDialog()
      }
    }

    isWindowsInstalled = UtilityHelper.isWindowsInstalled(this@MainActivity)
    isWindowsMounted = UtilityHelper.isWindowsMounted(this@MainActivity)
    isBackupExists = UtilityHelper.isBackupExists(
      "${Environment.getExternalStorageDirectory().path}/WINCross/Backup/logo.img"
    )
  }

  public fun updateUiBasedOnState() {
    uiStateManager.updateAll()
  }

  override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
      refreshFileSystemState()
      updateUiBasedOnState()
    }
  }

  private suspend fun refreshFileSystemState() = withContext(Dispatchers.IO) {
    updateFileSystemState(true)
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_toolbar, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_settings -> {
        startActivity(Intent(this, SettingsActivity::class.java))
        true
      } else -> super.onOptionsItemSelected(item)
    }
  }

  private suspend fun showUefiDialog() {
    if (isUefiDialogShowing) return
    isUefiDialogShowing = true

    try {
      DialogHelper.createDialog(
        context = this,
        rootView = window.decorView.rootView,
        title = getString(R.string.select_uefi),
        message = getString(R.string.select_uefi_message),
        positiveButtonText = getString(R.string.select_uefi),
        negativeButtonText = getString(R.string.cancel),
        onPositive = {
          UEFIHelper.initialize(this)
          UEFIHelper.selectUEFIFile()
          isUefiDialogShowing = false
        },
        onDismiss = {
          isUefiDialogShowing = false
        }
      )
    } catch (e: Exception) {
      Log.e(TAG, getString(R.string.error_showing_dialog, e.message))
      isUefiDialogShowing = false
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    permissionManager.handlePermissionResult(requestCode, permissions, grantResults)
  }

  public fun checkRoot(action: () -> Unit) {
    if (!UtilityHelper.isDeviceRooted()) {
      UtilityHelper.showToast(this, getString(R.string.error_not_rooted))
      return
    }
    action()
  }
}