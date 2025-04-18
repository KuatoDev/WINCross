package id.vern.wincross.managers

import android.content.SharedPreferences
import android.graphics.Color
import id.vern.wincross.R
import id.vern.wincross.activity.*
import id.vern.wincross.databinding.ActivityMainBinding
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.*
import java.text.SimpleDateFormat
import java.util.*

class UIStateManager(
    private val activity: MainActivity,
    private val binding: ActivityMainBinding,
    private val sharedPreferences: SharedPreferences
) {
  fun updateAll() {
    updateQuickbootButton()
    updateSelectUefiButton()
    updateDashboard()
    activity.mountManager.updateMountButtonState()
    updateToolbox()
    updateDeviceModels()
    updateDeviceSpecificUI()
  }

  private fun updateQuickbootButton() {
    binding.mainbutton.apply {
      tvSwitchToWindows.text =
          activity.getString(
              if (activity.isUefiAvailable) R.string.boot_to_windows else R.string.uefi_not_found)

      tvSwitchToWindows.setTextColor(
          if (activity.isUefiAvailable) tvSwitchToWindows.currentTextColor else Color.RED)
      btnSwitchToWindows.isEnabled = activity.isUefiAvailable
    }
  }

  private fun updateSelectUefiButton() {
    val path = UEFIHelper.getSavedUEFIPath()
    binding.mainbutton.tvSelectUefi.text =
        if (path != null) {
          val file = java.io.File(path)
          file.name
        } else {
          "No UEFI Selected"
        }
  }

  private fun updateDashboard() {
    updateDeviceInfo()
    updateWindowsStatus()
    updateBackupStatus()
  }

  private fun updateDeviceInfo() {
    binding.dashboard.woaTitle.isSelected = true
  }

  private fun updateWindowsStatus() {
    binding.dashboard.tvWindowsInstalled.text =
        activity.getString(
            R.string.windows_status,
            activity.getString(
                if (activity.isWindowsInstalled) R.string.status_installed
                else R.string.status_not_installed))
  }

  private fun updateBackupStatus() {
    with(binding.mainbutton) {
      if (activity.isBackupExists) {
        tvBackup.text = getLastBackupTime()
        btnRestore.isEnabled = true
        tvRestore.text = activity.getString(R.string.restore_button_text)
        tvBackup.setTextColor(tvBackup.currentTextColor) // Reset warna
        tvRestore.setTextColor(tvRestore.currentTextColor) // Reset warna
      } else {
        tvBackup.text = activity.getString(R.string.backup_not_found)
        tvBackup.setTextColor(Color.RED)
        btnRestore.isEnabled = false
        tvRestore.text = activity.getString(R.string.backup_not_found)
        tvRestore.setTextColor(Color.RED)
      }
    }
  }

  private fun getLastBackupTime(): String {
    val lastBackupTime = sharedPreferences.getLong(MainActivity.LAST_BACKUP_KEY, 0L)
    return if (lastBackupTime > 0L) {
      val dateFormat = SimpleDateFormat("hh:mm a, MMM dd, yyyy", Locale.US)
      dateFormat.timeZone = TimeZone.getDefault()
      dateFormat.format(Date(lastBackupTime))
    } else {
      activity.getString(R.string.backup_not_found)
    }
  }

  private fun updateToolbox() {
    binding.mainbutton.apply {
      btnToolbox.isEnabled = activity.isWindowsMounted
      tvToolbox.text =
          if (activity.isWindowsMounted) {
            activity.getString(R.string.toolbox_summary)
          } else {
            activity.getString(R.string.mount_windows_for_toolbox)
          }
    }
  }

  private fun updateDeviceModels() {
    DeviceModels.setDeviceInfo(
        activity,
        binding.dashboard.tvDevice,
        binding.dashboard.deviceimage,
        binding.dashboard.tvTotalRam,
        binding.dashboard.tvTotalStorage,
        binding.dashboard.tvPanel,
        binding.dashboard.tvActiveSlot,
        binding.dashboard.tvBatteryCapacity,
        binding.dashboard.tvKernelPowerProfile,
        binding.dashboard.btnGuide,
        binding.dashboard.btnGroup)
  }

  private fun updateDeviceSpecificUI() {
    activity.uefiUpdateManager.checkForUpdates()
  }
}
