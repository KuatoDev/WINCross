package id.vern.wincross.widgets

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import id.vern.wincross.R
import id.vern.wincross.operations.*
import id.vern.wincross.helpers.*
import kotlinx.coroutines.*

class QuickMountDialogActivity : Activity() {
  private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val isMounted = intent.getBooleanExtra("is_mounted", false)
    if (isMounted) {
      showUmountDialog()
    } else {
      showMountDialog()
    }
  }

  private fun showMountDialog() {
    MaterialAlertDialogBuilder(this)
    .setTitle(getString(R.string.mount_windows))
    .setMessage(getString(R.string.msg_mount_windows))
    .setPositiveButton(getString(R.string.yes)) {
      _, _ ->
      coroutineScope.launch(Dispatchers.IO) {
        val success = MountWindows.mount(this@QuickMountDialogActivity)
        withContext(Dispatchers.Main) {
          if (success) {
            UtilityHelper.showToast(
              this@QuickMountDialogActivity,
              getString(R.string.windows_mounted_success)
            )
            // Update widget
            QuickBootWidgetProvider.updateAllWidgets(this@QuickMountDialogActivity)
          } else {
            UtilityHelper.showToast(
              this@QuickMountDialogActivity,
              getString(R.string.windows_mount_failed)
            )
          }
          finish()
        }
      }
    }
    .setNegativeButton(getString(R.string.cancel)) {
      _, _ ->
      finish()
    }
    .setOnCancelListener {
      finish()
    }
    .show()
  }

  private fun showUmountDialog() {
    MaterialAlertDialogBuilder(this)
    .setTitle(getString(R.string.umount_windows))
    .setMessage(getString(R.string.msg_umount_windows))
    .setPositiveButton(getString(R.string.yes)) {
      _, _ ->
      coroutineScope.launch(Dispatchers.IO) {
        val success = MountWindows.umount(this@QuickMountDialogActivity)
        withContext(Dispatchers.Main) {
          if (success) {
            UtilityHelper.showToast(
              this@QuickMountDialogActivity,
              getString(R.string.windows_umounted_success)
            )
            DialogHelper.showPopupNotifications(
              this@QuickMountDialogActivity,
              "Windows unmounted"
            )
            // Update widget
            QuickBootWidgetProvider.updateAllWidgets(this@QuickMountDialogActivity)
          } else {
            UtilityHelper.showToast(
              this@QuickMountDialogActivity,
              getString(R.string.windows_umount_failed)
            )
            DialogHelper.showPopupNotifications(
              this@QuickMountDialogActivity,
              "Failed to unmount Windows"
            )
          }
          finish()
        }
      }
    }
    .setNegativeButton(getString(R.string.cancel)) {
      _, _ ->
      finish()
    }
    .setOnCancelListener {
      finish()
    }
    .show()
  }

  override fun onDestroy() {
    coroutineScope.cancel()
    super.onDestroy()
  }
}