package id.vern.wincross.handlers

import android.content.*
import android.os.Environment
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.UtilityHelper

class SwitchHandler(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) {
  companion object {
    private const val TAG = "SwitchHandler"
    private const val MOUNT_TO_MNT_KEY = "Windows Mount Path"
  }

  private val switchConfigs =
      mapOf(
          "force_backup_to_win" to
              SwitchConfig(
                  key = "force_backup_to_win", featureNameId = R.string.feature_backup_to_win),
          "backup_boot_if_empty" to
              SwitchConfig(
                  key = "backup_boot_if_empty",
                  featureNameId = R.string.feature_backup_boot_if_empty),
          "uefi_auto_update" to
              SwitchConfig(
                  key = "uefi_auto_update", featureNameId = R.string.feature_backup_boot_if_empty),
          "automatic_mount_windows" to
              SwitchConfig(
                  key = "automatic_mount_windows",
                  featureNameId = R.string.feature_automatic_mount),
          MOUNT_TO_MNT_KEY to
              SwitchConfig(key = MOUNT_TO_MNT_KEY, featureNameId = R.string.feature_mount_to_mnt),
          "always_provision_modem" to
              SwitchConfig(
                  key = "always_provision_modem", featureNameId = R.string.feature_provision_modem))

  private data class SwitchConfig(val key: String, val featureNameId: Int? = null)

  private fun handleSwitch(key: String, isChecked: Boolean) {
    val config = switchConfigs[key] ?: return
    if (key == MOUNT_TO_MNT_KEY) {
      val windowsPath =
          if (isChecked) {
            "/mnt/Windows"
          } else {
            "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"
          }

      sharedPreferences.edit().putString(MOUNT_TO_MNT_KEY, windowsPath).apply()

      Log.d(TAG, "Updated Windows Mount Path path to: $windowsPath")
    } else {
      sharedPreferences.edit().putBoolean(config.key, isChecked).apply()
    }
    Log.d(TAG, "handleSwitch: ${config.key} updated to $isChecked")
  }

  fun handleBackupToWin(isChecked: Boolean) = handleSwitch("force_backup_to_win", isChecked)

  fun handleBackupBootIfEmpty(isChecked: Boolean) = handleSwitch("backup_boot_if_empty", isChecked)

  fun handleAutomaticMountWindows(isChecked: Boolean) =
      handleSwitch("automatic_mount_windows", isChecked)

  fun handleMountToMnt(isChecked: Boolean) = handleSwitch(MOUNT_TO_MNT_KEY, isChecked)

  fun handleUEFIAutoUpdate(isChecked: Boolean) = handleSwitch("uefi_auto_update", isChecked)

  fun handleProvisionModem(isChecked: Boolean) = handleSwitch("always_provision_modem", isChecked)
}
