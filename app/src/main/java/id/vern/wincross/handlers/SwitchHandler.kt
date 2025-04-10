package id.vern.wincross.handlers

import android.content.*
import android.util.Log
import id.vern.wincross.helpers.UtilityHelper
import id.vern.wincross.R

class SwitchHandler(
  private val context: Context,
  private val sharedPreferences: SharedPreferences
) {
  companion object {
    private const val TAG = "SwitchHandler"
  }

  // Define switch configurations
  private val switchConfigs = mapOf(
    "force_backup_to_win" to SwitchConfig(
      key = "force_backup_to_win",
      enabledMessageId = R.string.toast_backup_to_win_enabled,
      disabledMessageId = R.string.toast_backup_to_win_disabled
    ),
    "backup_boot_if_empty" to SwitchConfig(
      key = "backup_boot_if_empty",
      enabledMessageId = R.string.toast_backup_boot_if_empty_enabled,
      disabledMessageId = R.string.toast_backup_boot_if_empty_disabled
    ),
    "uefi_auto_update" to SwitchConfig(
      key = "uefi_auto_update",
      enabledMessageId = R.string.toast_backup_boot_if_empty_enabled,
      disabledMessageId = R.string.toast_backup_boot_if_empty_disabled
    ),
    "flash_logo_with_uefi" to SwitchConfig(
      key = "flash_logo_with_uefi",
      enabledMessageId = R.string.toast_flash_logo_enabled,
      disabledMessageId = R.string.toast_flash_logo_disabled
    ),
    "automatic_mount_windows" to SwitchConfig(
      key = "automatic_mount_windows",
      enabledMessageId = R.string.toast_automatic_mount_windows_enabled,
      disabledMessageId = R.string.toast_automatic_mount_windows_disabled
    ),
    "mount_to_mnt" to SwitchConfig(
      key = "mount_to_mnt",
      enabledMessage = "Mount Windows to mnt/Windows enabled",
      disabledMessage = "Mount Windows to mnt/Windows disabled"
    ),
    "always_provision_modem" to SwitchConfig(
      key = "always_provision_modem",
      enabledMessage = "Always provision modem enabled",
      disabledMessage = "Always provision modem disabled"
    )
  )

  private data class SwitchConfig(
    val key: String,
    val enabledMessageId: Int? = null,
    val disabledMessageId: Int? = null,
    val enabledMessage: String? = null,
    val disabledMessage: String? = null
  )

  private fun handleSwitch(key: String, isChecked: Boolean) {
    val config = switchConfigs[key] ?: return

    // Update preference
    sharedPreferences.edit()
    .putBoolean(config.key, isChecked)
    .apply()

    // Log change
    Log.d(TAG, "handleSwitch: ${config.key} updated to $isChecked")

    // Show toast
    val message = when {
      isChecked && config.enabledMessageId != null -> context.getString(config.enabledMessageId)
      !isChecked && config.disabledMessageId != null -> context.getString(config.disabledMessageId)
      isChecked && config.enabledMessage != null -> config.enabledMessage
      !isChecked && config.disabledMessage != null -> config.disabledMessage
      else -> return
    }
    UtilityHelper.showToast(context, message)
  }

  // Public methods using the generic handler
  fun handleBackupToWin(isChecked: Boolean) =
  handleSwitch("force_backup_to_win", isChecked)

  fun handleBackupBootIfEmpty(isChecked: Boolean) =
  handleSwitch("backup_boot_if_empty", isChecked)

  fun handleFlashLogoWithUefi(isChecked: Boolean) =
  handleSwitch("flash_logo_with_uefi", isChecked)

  fun handleAutomaticMountWindows(isChecked: Boolean) =
  handleSwitch("automatic_mount_windows", isChecked)

  fun handleMountToMnt(isChecked: Boolean) =
  handleSwitch("mount_to_mnt", isChecked)

  fun handleUEFIAutoUpdate(isChecked: Boolean) =
  handleSwitch("uefi_auto_update", isChecked)

  fun handleProvisionModem(isChecked: Boolean) =
  handleSwitch("always_provision_modem", isChecked)
}