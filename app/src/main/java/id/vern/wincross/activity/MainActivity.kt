package id.vern.wincross.activity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import id.vern.wincross.R
import id.vern.wincross.databinding.ActivityMainBinding
import id.vern.wincross.handlers.*
import id.vern.wincross.helpers.*
import id.vern.wincross.operations.*
import id.vern.wincross.utils.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import android.app.AlarmManager
import androidx.activity.result.ActivityResultLauncher

/**
 * MainActivity for WinCross application
 * Handles Windows dual boot functionality, UEFI flashing, and system backups
 *
 * @author KuatoDev
 * @since 2025-04-06 08:41:43 UTC
 */
class MainActivity : AppCompatActivity() {

  // Region: Properties and Constants
  private lateinit var filePickerLauncher: ActivityResultLauncher<String>
  private lateinit var binding: ActivityMainBinding
  private lateinit var sharedPreferences: SharedPreferences
  private val switchHandler by lazy {
    SwitchHandler(this, sharedPreferences)
  }

  private var isUefiAvailable: Boolean = false
  private var isWindowsInstalled: Boolean = false
  private var isWindowsMounted: Boolean = false
  private var isBackupExists: Boolean = false

  companion object {
    private const val TAG = "MainActivity"
    private const val PREFS_NAME = "WinCross_preferences"
    private const val LAST_BACKUP_KEY = "last_backup_time"
    private const val PERMISSION_REQUEST_CODE = 1001
    private const val EXACT_ALARM_REQUESTED_KEY = "exact_alarm_requested"
  }

  // Permission handler for notification
  private val requestPermissionLauncher =
  registerForActivityResult(ActivityResultContracts.RequestPermission()) {
    isGranted ->
    if (isGranted) {
      Log.d(TAG, "Notification permission granted")
    } else {
      Log.d(TAG, "Notification permission denied")
    }
  }

  // Region: Lifecycle Methods
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initializeTheme()
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    checkAndRequestPermissions()
    setupToolbar()

    filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
      uri: Uri? ->
      uri?.let {
        UEFIHelper.handleSelectedFile(it)
      }
    }

    UEFIHelper.initialize(this, filePickerLauncher)

    setupButtonActions()
    setupSwitches()

    lifecycleScope.launch {
      checkFilesAndPermissions()
      updateUiBasedOnState()
    }
  }

  override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
      refreshFileSystemState()
      updateUiBasedOnState()
    }
  }

  // Region: Theme Management
  private fun initializeTheme() {
    sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val themeColor = sharedPreferences.getString(getString(R.string.key_theme_color), "default")
    val theme = sharedPreferences.getString(getString(R.string.key_theme), "default")

    val themeId = when (themeColor) {
      "blue" -> R.style.Theme_MyApp_Blue
      "red" -> R.style.Theme_MyApp_Red
      "green" -> R.style.Theme_MyApp_Green
      "yellow" -> R.style.Theme_MyApp_Yellow
      else -> R.style.Theme_MyApp_Default
    }

    val mode = when (theme) {
      "dark" -> AppCompatDelegate.MODE_NIGHT_YES
      "light" -> AppCompatDelegate.MODE_NIGHT_NO
      else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    AppCompatDelegate.setDefaultNightMode(mode)
    setTheme(themeId)
  }

  // Region: Menu Management
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

  private fun setupToolbar() {
    binding.toolbarlayout.toolbar.apply {
      title = getString(R.string.toolbar_title)
      setSupportActionBar(this)
      subtitle = "Cross-Platform Windows Control"
      setNavigationIcon(R.drawable.ic_toolbar)
    }
  }

  // Region: File System and Permissions Check
  private suspend fun checkFilesAndPermissions() = withContext(Dispatchers.IO) {
    updateFileSystemState()
    checkRequiredPermissions()
  }

  private suspend fun updateFileSystemState() {
    val backupPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Backup/logo.img"
    val selectedUefiPath = sharedPreferences.getString("UEFI", null)

    isUefiAvailable = if (selectedUefiPath.isNullOrEmpty()) {
      UtilityHelper.isUefiFileAvailable(this@MainActivity)
    } else {
      true
    }

    isWindowsInstalled = UtilityHelper.isWindowsInstalled(this@MainActivity)
    isWindowsMounted = UtilityHelper.isWindowsMounted(this@MainActivity)
    isBackupExists = UtilityHelper.isBackupExists(backupPath)
  }

  private suspend fun checkRequiredPermissions() = withContext(Dispatchers.Main) {
    checkAndRequestManageAllFilesPermission()
    checkNotificationPermission()
    checkScheduleExactAlarmPermissionIfNeeded()
    checkAutoMountIfNeeded()
  }

  private fun checkScheduleExactAlarmPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
      !sharedPreferences.getBoolean(EXACT_ALARM_REQUESTED_KEY, false)
    ) {
      checkAndRequestScheduleExactAlarmPermission()
    }
  }

  private fun checkAutoMountIfNeeded() {
    if (isWindowsInstalled &&
      sharedPreferences.getBoolean("automatic_mount_windows", false) &&
      !isWindowsMounted
    ) {
      attemptAutoMount()
    }
  }

  private suspend fun refreshFileSystemState() = withContext(Dispatchers.IO) {
    updateFileSystemState()
  }

  // Region: Permission Handling
  private fun checkAndRequestManageAllFilesPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
      try {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
          data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to open permission request: ${e.message}")
      }
    }
  }

  private fun checkAndRequestScheduleExactAlarmPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
      if (!alarmManager.canScheduleExactAlarms() &&
        !sharedPreferences.getBoolean(EXACT_ALARM_REQUESTED_KEY, false)
      ) {
        try {
          val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
          }
          startActivity(intent)
          sharedPreferences.edit().putBoolean(EXACT_ALARM_REQUESTED_KEY, true).apply()
        } catch (e: Exception) {
          Log.e(TAG, "Failed to open permission request: ${e.message}")
        }
      }
    }
  }

  private fun hasExactAlarmPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
      alarmManager.canScheduleExactAlarms()
    } else {
      true
    }
  }

  // Region: Mount Operations
  private fun attemptAutoMount() {
    Log.d(TAG, "Automatic mount enabled, mounting Windows...")
    lifecycleScope.launch(Dispatchers.IO) {
      if (MountWindows.mount(this@MainActivity)) {
        Log.d(TAG, "Automatic mount succeeded.")
        isWindowsMounted = true
        withContext(Dispatchers.Main) {
          updateUiBasedOnState()
        }
      } else {
        Log.e(TAG, "Automatic mount failed.")
      }
    }
  }

  // Region: UI State Management
  private fun updateUiBasedOnState() {
    updateQuickbootButtonState()
    updateSelectUefiButtonState()
    updateDashboardInfo()
    updateMountButtonState()
    updateToolboxState()
    deviceModels()
  }

  private fun deviceModels() {
    DeviceModels.setDeviceInfo(
      this,
      binding.dashboard.tvDevice,
      binding.dashboard.deviceimage,
      binding.dashboard.tvTotalRam,
      binding.dashboard.tvTotalStorage,
      binding.dashboard.tvPanel,
      binding.dashboard.tvActiveSlot,
      binding.dashboard.tvBatteryCapacity,
      binding.dashboard.tvKernelPowerProfile,
      binding.dashboard.btnGuide,
      binding.dashboard.btnGroup
    )
  }

  private fun updateQuickbootButtonState() {
    binding.mainbutton.apply {
      tvSwitchToWindows.text = getString(
        if (isUefiAvailable) R.string.boot_to_windows
        else R.string.uefi_not_found
      )
      btnSwitchToWindows.isEnabled = isUefiAvailable
    }
  }

  private fun updateSelectUefiButtonState() {
    binding.mainbutton.tvSelectUefi.text =
    UEFIHelper.getSavedUEFIPath() ?: "No UEFI Selected"
  }

  private fun updateToolboxState() {
    binding.mainbutton.apply {
      btnToolbox.isEnabled = isWindowsMounted
      tvToolbox.text = if (isWindowsMounted) {
        getString(R.string.toolbox_summary)
      } else {
        getString(R.string.mount_windows_for_toolbox)
      }
    }
  }

  // Region: Dashboard Management
  private fun updateDashboardInfo() {
    updateDeviceInfo()
    updateWindowsStatus()
    updateDeviceSpecificUI()
    updateBackupStatus()
  }

  private fun updateDeviceInfo() {
    with(binding.dashboard) {
      woaTitle.isSelected = true
    }
  }

  private fun updateWindowsStatus() {
    binding.dashboard.tvWindowsInstalled.text = getString(
      if (isWindowsInstalled) R.string.windows_installed
      else R.string.windows_not_installed
    )
  }

  private var isShowingUpdateDialog = false

  private fun updateDeviceSpecificUI() {
    if (isShowingUpdateDialog) {
      Log.d("UpdateUI", "Update dialog is already showing, skipping check")
      return
    }

    val deviceModel = getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    .getString("device_model", null) ?: Utils.getDeviceModel(this)

    Log.d("UpdateUI", "Detected device model: $deviceModel")

    when {
      deviceModel.contains("vayu", true) -> {
        Log.d("UpdateUI", "Checking updates for POCO X3 Pro (bhima)")
        checkForUpdates("woa-vayu", "POCOX3Pro-Releases")
      }
      deviceModel.contains("nabu", true) -> {
        Log.d("UpdateUI", "Checking updates for Xiaomi Pad 5")
        checkForUpdates("erdilS", "Port-Windows-11-Xiaomi-Pad-5")
      }
      deviceModel.contains("a52s", true) -> {
        Log.d("UpdateUI", "Checking updates for Samsung A52s 5G")
        checkForUpdates("woa-a52s", "Samsung-A52s-5G-Releases")
      }
      deviceModel.contains("beryllium", true) -> {
        Log.d("UpdateUI", "Checking updates for POCO F1")
        checkForUpdates("n00b69", "woa-beryllium")
      }
      deviceModel.contains("cepheus", true) -> {
        Log.d("UpdateUI", "Checking updates for Mi 9")
        checkForUpdates("qaz6750", "XiaoMi9-Drivers")
      } else -> {
        Log.d("UpdateUI", "No update check configured for this device model")
      }
    }
  }

  private fun checkForUpdates(owner: String, repo: String) {
    Log.d("UpdateCheck", "Starting update check for $owner/$repo")
    isShowingUpdateDialog = true

    lifecycleScope.launch {
      try {
        Log.d("UpdateCheck", "Fetching updates from GitHub...")
        val (hasUpdate, latestVersion, downloadUrl) = UEFIHelper.checkForUpdates(owner, repo)
        Log.d("UpdateCheck", "Update check result - hasUpdate: $hasUpdate, latestVersion: $latestVersion, downloadUrl: $downloadUrl")

        if (hasUpdate && downloadUrl.isNotEmpty()) {
          withContext(Dispatchers.Main) {
            val currentVersion = UEFIHelper.getCurrentVersion()
            Log.d("UpdateCheck", "Current version: $currentVersion")

            if (!isFinishing && !isDestroyed) {
              Log.d("UpdateCheck", "Showing update dialog")
              DialogHelper.createDialog(
                context = this@MainActivity,
                rootView = window.decorView.rootView,
                title = "Update Available",
                message = """
                                A new version of UEFI is available!
                                Current version: v$currentVersion
                                Latest version: v$latestVersion
                                Would you like to download the update now?
                            """.trimIndent(),
                positiveButtonText = "Download",
                negativeButtonText = "Later",
                onPositive = {
                  try {
                    Log.d("UpdateCheck", "Opening download URL: $downloadUrl")
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                      data = Uri.parse(downloadUrl)
                      flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                  } catch (e: Exception) {
                    Log.e("UpdateCheck", "Failed to open download link", e)
                    UtilityHelper.showToast(
                      this@MainActivity,
                      "Failed to open download link"
                    )
                  }
                },
                onDismiss = {
                  Log.d("UpdateCheck", "Update dialog dismissed")
                  isShowingUpdateDialog = false
                }
              )
            } else {
              Log.d("UpdateCheck", "Activity is finishing or destroyed, cannot show dialog")
              isShowingUpdateDialog = false
            }
          }
        } else {
          Log.d("UpdateCheck", "No updates available or empty download URL")
          isShowingUpdateDialog = false
        }
      } catch (e: Exception) {
        Log.e("UpdateCheck", "Error during update check", e)
        withContext(Dispatchers.Main) {
          UtilityHelper.showToast(
            this@MainActivity,
            "Failed to check for updates: ${e.message}"
          )
        }
        isShowingUpdateDialog = false
      }
    }
  }

  private fun updateBackupStatus() {
    with(binding.mainbutton) {
      if (isBackupExists) {
        tvBackup.text = getString(R.string.last_backup_label, getLastBackupTime())
        btnRestore.isEnabled = true
        tvRestore.text = getString(R.string.restore_button_text)
      } else {
        tvBackup.text = getString(R.string.backup_not_found)
        btnRestore.isEnabled = false
        tvRestore.text = getString(R.string.backup_not_found)
      }
    }
  }

  private fun getLastBackupTime(): String {
    val lastBackupTime = sharedPreferences.getLong(LAST_BACKUP_KEY, 0L)
    return if (lastBackupTime > 0L) {
      SimpleDateFormat("hh:mm, dd MMMM yyyy", Locale.getDefault())
      .format(Date(lastBackupTime))
    } else {
      getString(R.string.backup_not_found)
    }
  }

  // Region: Button Setup
  private fun setupButtonActions() {
    with(binding.mainbutton) {
      // Set text marquee
      tvBackup.isSelected = true
      tvToolbox.isSelected = true
      tvMountWindows.isSelected = true
      tvSwitchToWindows.isSelected = true
      tvSelectUefi.isSelected = true
      tvRestore.isSelected = true
      // Set click listeners
      btnBackup.setOnClickListener {
        handleBackupButtonClick()
      }
      btnRestore.setOnClickListener {
        handleRestoreButtonClick()
      }
      btnSelectUEFI.setOnClickListener {
        handleSelectUEFIButtonClick()
      }
      btnSwitchToWindows.setOnClickListener {
        handleSwitchToWindowsButtonClick()
      }
      btnToolbox.setOnClickListener {
        startActivity(Intent(this@MainActivity, ToolboxActivity::class.java))
      }
    }
  }

  private fun setupSwitches() {
    with(binding.mainpreferences) {
      msForceBackupToWin.isChecked = sharedPreferences.getBoolean("force_backup_to_win", false)
      msBackupBootIfEmpty.isChecked = sharedPreferences.getBoolean("backup_boot_if_empty", false)
      msAutomaticMountWindows.isChecked = sharedPreferences.getBoolean("automatic_mount_windows", false)
      msMountToMnt.isChecked = sharedPreferences.getBoolean("mount_to_mnt", false)
      msFlashLogoWithUefi.isChecked = sharedPreferences.getBoolean("flash_logo_with_uefi", false)
      msAlwaysProvisionModem.isChecked = sharedPreferences.getBoolean("always_provision_modem", false)
      val model = getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
      .getString("device_model", null)?.lowercase()

      msFlashLogoWithUefi.visibility = if (
        model?.contains("vayu", true) == true || model?.contains("bhima", true) == true
      ) View.VISIBLE else View.GONE
      msFlashLogoWithUefi.setOnCheckedChangeListener {
        _, isChecked ->
        switchHandler.handleFlashLogoWithUefi(isChecked)
      }
      msAlwaysProvisionModem.setOnCheckedChangeListener {
        _, isChecked ->
        switchHandler.handleProvisionModem(isChecked)
      }
      msForceBackupToWin.setOnCheckedChangeListener {
        _, isChecked ->
        switchHandler.handleBackupToWin(isChecked)
      }
      msBackupBootIfEmpty.setOnCheckedChangeListener {
        _, isChecked ->
        switchHandler.handleBackupBootIfEmpty(isChecked)
      }
      msAutomaticMountWindows.setOnCheckedChangeListener {
        _, isChecked ->
        switchHandler.handleAutomaticMountWindows(isChecked)
      }
      msMountToMnt.setOnCheckedChangeListener {
        _, isChecked ->
        switchHandler.handleMountToMnt(isChecked)
      }
    }
  }

  // Region: Mount Button Management
  private fun updateMountButtonState() {
    if (!isWindowsInstalled) {
      binding.mainbutton.apply {
        tvMountWindows.text = getString(R.string.windows_not_installed)
        btnMountWindows.isEnabled = false
      }
      return
    }

    if (isWindowsMounted) {
      setupUmountWindows()
    } else {
      setupMountWindows()
    }
  }

  private fun setupMountWindows() {
    binding.mainbutton.apply {
      tvMountWindows.text = getString(R.string.mount_windows)
      btnMountWindows.isEnabled = true
      btnMountWindows.setOnClickListener {
        DialogHelper.showMountWindowsPopup(this@MainActivity, window.decorView.rootView) {
          lifecycleScope.launch(Dispatchers.IO) {
            val success = MountWindows.mount(this@MainActivity)
            withContext(Dispatchers.Main) {
              if (success) {
                UtilityHelper.showToast(
                  this@MainActivity,
                  getString(R.string.windows_mounted_success)
                )
                isWindowsMounted = true
                updateUiBasedOnState()
              } else {
                UtilityHelper.showToast(
                  this@MainActivity,
                  getString(R.string.windows_mount_failed)
                )
              }
            }
          }
        }
      }
    }
  }

  private fun setupUmountWindows() {
    binding.mainbutton.apply {
      tvMountWindows.text = getString(R.string.umount_windows)
      btnMountWindows.isEnabled = true
      btnMountWindows.setOnClickListener {
        DialogHelper.showUmountWindowsPopup(this@MainActivity, window.decorView.rootView) {
          lifecycleScope.launch(Dispatchers.IO) {
            val success = MountWindows.umount(this@MainActivity)
            withContext(Dispatchers.Main) {
              if (success) {
                UtilityHelper.showToast(
                  this@MainActivity,
                  getString(R.string.windows_umounted_success)
                )
                DialogHelper.showPopupNotifications(
                  this@MainActivity,
                  "Windows unmounted"
                )
                isWindowsMounted = false
                updateUiBasedOnState()
              } else {
                UtilityHelper.showToast(
                  this@MainActivity,
                  getString(R.string.windows_umount_failed)
                )
                DialogHelper.showPopupNotifications(
                  this@MainActivity,
                  "Failed to unmount Windows"
                )
              }
            }
          }
        }
      }
    }
  }

  // Region: Permission Checks
  private fun checkNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = Manifest.permission.POST_NOTIFICATIONS
      if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(permission)
      }
    }
  }

  private fun checkAndRequestPermissions() {
    val permissions = listOf(
      Manifest.permission.RECEIVE_BOOT_COMPLETED
    )

    val permissionsToRequest = permissions.filter {
      ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }

    if (permissionsToRequest.isNotEmpty()) {
      ActivityCompat.requestPermissions(
        this,
        permissionsToRequest.toTypedArray(),
        PERMISSION_REQUEST_CODE
      )
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PERMISSION_REQUEST_CODE) {
      val deniedPermissions = permissions.withIndex()
      .filter {
        grantResults[it.index] != PackageManager.PERMISSION_GRANTED
      }
      if (deniedPermissions.isNotEmpty()) {
        deniedPermissions.forEach {
          Log.d(TAG, "Permission denied: ${it.value}")
        }
      } else {
        Log.d(TAG, "All permissions granted")
      }
    }
  }

  // Region: Button Click Handlers
  private fun handleBackupButtonClick() {
    checkRoot {
      DialogHelper.showBackupKernelPopup(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.IO) {
          BackupOperation.backupAll(this@MainActivity)
          sharedPreferences.edit()
          .putLong(LAST_BACKUP_KEY, System.currentTimeMillis())
          .apply()
          withContext(Dispatchers.Main) {
            isBackupExists = true
            updateDashboardInfo()
          }
        }
      }
    }
  }

  private fun handleSwitchToWindowsButtonClick() {
    checkRoot {
      DialogHelper.showFlashUefiPopup(this, window.decorView.rootView) {
        FlashOperation.flashIt(this@MainActivity)
      }
    }
  }

  private fun handleRestoreButtonClick() {
    startActivity(Intent(this@MainActivity, RestoreBackupActivity::class.java))
  }

  private fun handleSelectUEFIButtonClick() {
    DialogHelper.showUEFIOptionsDialog(this@MainActivity, window.decorView.rootView) {
      lifecycleScope.launch {
        val selectedUefiPath = sharedPreferences.getString("UEFI", null)
        isUefiAvailable = if (selectedUefiPath.isNullOrEmpty()) {
          UtilityHelper.isUefiFileAvailable(this@MainActivity)
        } else {
          true
        }
        updateSelectUefiButtonState()
        updateQuickbootButtonState()
      }
    }
  }

  private fun checkRoot(action: () -> Unit) {
    if (!UtilityHelper.isDeviceRooted()) {
      UtilityHelper.showToast(this, getString(R.string.device_not_rooted))
      return
    }
    action()
  }
}