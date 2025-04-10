package id.vern.wincross.handlers

import android.content.*
import android.util.Log
import id.vern.wincross.helpers.UtilityHelper
import id.vern.wincross.R

class SwitchHandler(private val context: Context, private val sharedPreferences: SharedPreferences) {

  fun handleBackupToWin(isChecked: Boolean) {
    sharedPreferences.edit().putBoolean("force_backup_to_win", isChecked).apply()
    Log.d("SwitchHandler", "handleBackupToWin: backup_to_win updated to $isChecked")
    if (isChecked) {
      UtilityHelper.showToast(context, context.getString(R.string.toast_backup_to_win_enabled))
    } else {
      UtilityHelper.showToast(context, context.getString(R.string.toast_backup_to_win_disabled))
    }
  }

  fun handleBackupBootIfEmpty(isChecked: Boolean) {
    sharedPreferences.edit().putBoolean("backup_boot_if_empty", isChecked).apply()
    Log.d("SwitchHandler", "handleBackupBootIfEmpty: backup_boot_if_empty updated to $isChecked")
    if (isChecked) {
      UtilityHelper.showToast(context, context.getString(R.string.toast_backup_boot_if_empty_enabled))
    } else {
      UtilityHelper.showToast(context, context.getString(R.string.toast_backup_boot_if_empty_disabled))
    }
  }

  fun handleFlashLogoWithUefi(isChecked: Boolean) {
    sharedPreferences.edit().putBoolean("flash_logo_with_uefi", isChecked).apply()
    Log.d("SwitchHandler", "handleBackupBootIfEmpty: flash_logo_with_uefi updated to $isChecked")
    if (isChecked) {
      UtilityHelper.showToast(context, context.getString(R.string.toast_flash_logo_enabled))
    } else {
      UtilityHelper.showToast(context, context.getString(R.string.toast_flash_logo_disabled))
    }
  }

  fun handleAutomaticMountWindows(isChecked: Boolean) {
    sharedPreferences.edit().putBoolean("automatic_mount_windows", isChecked).apply()
    Log.d("SwitchHandler", "handleAutomaticMountWindows: automatic_mount_windows updated to $isChecked")
    if (isChecked) {
      UtilityHelper.showToast(context, context.getString(R.string.toast_automatic_mount_windows_enabled))
    } else {
      UtilityHelper.showToast(context, context.getString(R.string.toast_automatic_mount_windows_disabled))
    }
  }

  fun handleMountToMnt(isChecked: Boolean) {
    sharedPreferences.edit().putBoolean("mount_to_mnt", isChecked).apply()
    Log.d("SwitchHandler", "handleMountToMnt: mount_to_mnt updated to $isChecked")
    if (isChecked) {
      UtilityHelper.showToast(context, "Mount Windows to mnt/Windows enabled")
    } else {
      UtilityHelper.showToast(context, "Mount Windows to mnt/Windows disabled")
    }
  }

  fun handleProvisionModem(isChecked: Boolean) {
    sharedPreferences.edit().putBoolean("always_provision_modem", isChecked).apply()
    Log.d("SwitchHandler", "handleMountToMnt: always_provision_modem updated to $isChecked")
    if (isChecked) {
      UtilityHelper.showToast(context, "Always provision modem enabled")
    } else {
      UtilityHelper.showToast(context, "Always provision modem disabled")
    }
  }
}