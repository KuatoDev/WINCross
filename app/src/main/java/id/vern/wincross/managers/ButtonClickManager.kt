package id.vern.wincross.managers

import android.content.*
import androidx.lifecycle.lifecycleScope
import id.vern.wincross.activity.*
import id.vern.wincross.helpers.*
import id.vern.wincross.operations.*
import id.vern.wincross.utils.*
import kotlinx.coroutines.*

class ButtonClickManager(private val activity: MainActivity) {
  private val lifecycleScope = activity.lifecycleScope
  private val prefs = activity.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
  private val rootView = activity.window.decorView.rootView

  fun handleBackup() {
    activity.checkRoot {
      DialogHelper.showBackupKernelPopup(activity, rootView) {
        lifecycleScope.launch(Dispatchers.IO) {
          BackupOperation.backupAll(activity)
          prefs.edit().putLong(MainActivity.LAST_BACKUP_KEY, System.currentTimeMillis()).apply()
          withContext(Dispatchers.Main) {
            activity.isBackupExists = true
            activity.uiStateManager.updateAll()
          }
        }
      }
    }
  }

  fun handleSwitchToWindows() {
    activity.checkRoot {
      DialogHelper.showFlashUefiPopup(activity, rootView) { FlashOperation.flashIt(activity) }
    }
  }

  fun handleRestore() {
    activity.startActivity(Intent(activity, RestoreBackupActivity::class.java))
  }

  fun handleSelectUEFI() {
    DialogHelper.showUEFIOptionsDialog(activity, rootView) {
      lifecycleScope.launch {
        val selectedUefiPath = prefs.getString("UEFI", null)
        activity.isUefiAvailable =
            if (selectedUefiPath.isNullOrEmpty()) {
              UtilityHelper.isUefiFileAvailable(activity)
            } else {
              true
            }
        activity.uiStateManager.updateAll()
      }
    }
  }
}
