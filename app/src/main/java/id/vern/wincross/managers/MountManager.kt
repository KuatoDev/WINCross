package id.vern.wincross.managers

import android.content.Context
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import id.vern.wincross.R
import id.vern.wincross.activity.MainActivity
import id.vern.wincross.activity.ToolboxActivity
import id.vern.wincross.helpers.DialogHelper
import id.vern.wincross.helpers.UtilityHelper
import id.vern.wincross.operations.MountWindows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MountManager(private val activity: MainActivity) {
    private val rootView = activity.window.decorView.rootView

    fun updateMountButtonState() {
        if (!activity.isWindowsInstalled) {
            setupDisabledMountButton()
            return
        }

        if (activity.isWindowsMounted) {
            setupUmountButton()
        } else {
            setupMountButton()
        }
    }

    private fun setupDisabledMountButton() {
        activity.binding.mainbutton.apply {
            tvMountWindows.text = activity.getString(
                R.string.windows_status,
                activity.getString(R.string.status_not_installed)
            )
            btnMountWindows.isEnabled = false
        }
    }

    private fun setupMountButton() {
        activity.binding.mainbutton.apply {
            tvMountWindows.text = activity.getString(R.string.mount_windows)
            btnMountWindows.isEnabled = true
            btnMountWindows.setOnClickListener {
                showMountDialog()
            }
        }
    }

    private fun setupUmountButton() {
        activity.binding.mainbutton.apply {
            tvMountWindows.text = activity.getString(R.string.umount_windows)
            btnMountWindows.isEnabled = true
            btnMountWindows.setOnClickListener {
                showUnmountDialog()
            }
        }
    }

    private fun showMountDialog() {
        DialogHelper.showMountWindowsPopup(activity, rootView) {
            activity.lifecycleScope.launch(Dispatchers.IO) {
                mount()
            }
        }
    }

    private fun showUnmountDialog() {
        DialogHelper.showUmountWindowsPopup(activity, rootView) {
            activity.lifecycleScope.launch(Dispatchers.IO) {
                unmount()
            }
        }
    }

    suspend fun mount() {
        if (MountWindows.mount(activity)) {
            handleSuccessfulMount()
        } else {
            handleFailedMount()
        }
    }

    suspend fun unmount() {
        if (MountWindows.umount(activity)) {
            handleSuccessfulUnmount()
        } else {
            handleFailedUnmount()
        }
    }

    private suspend fun handleSuccessfulMount() {
        activity.isWindowsMounted = true
        withContext(Dispatchers.Main) {
            showMountSuccess()
            activity.updateUiBasedOnState()
            checkAutoToolbox()
        }
    }

    private suspend fun handleFailedMount() {
        withContext(Dispatchers.Main) {
            UtilityHelper.showToast(
                activity,
                activity.getString(
                    R.string.mount_status,
                    activity.getString(R.string.action_mount),
                    activity.getString(R.string.status_failed)
                )
            )
        }
    }

    private suspend fun handleSuccessfulUnmount() {
        activity.isWindowsMounted = false
        withContext(Dispatchers.Main) {
            showUnmountSuccess()
            activity.updateUiBasedOnState()
        }
    }

    private suspend fun handleFailedUnmount() {
        withContext(Dispatchers.Main) {
            UtilityHelper.showToast(
                activity,
                activity.getString(
                    R.string.mount_status,
                    activity.getString(R.string.action_unmount),
                    activity.getString(R.string.status_failed)
                )
            )
            DialogHelper.showPopupNotifications(
                activity,
                "Failed to unmount Windows"
            )
        }
    }

    private fun showMountSuccess() {
        UtilityHelper.showToast(
            activity,
            activity.getString(
                R.string.mount_status,
                activity.getString(R.string.action_mount),
                activity.getString(R.string.status_success)
            )
        )
    }

    private fun showUnmountSuccess() {
        UtilityHelper.showToast(
            activity,
            activity.getString(
                R.string.mount_status,
                activity.getString(R.string.action_unmount),
                activity.getString(R.string.status_success)
            )
        )
        DialogHelper.showPopupNotifications(
            activity,
            "Windows unmounted"
        )
    }

    private fun checkAutoToolbox() {
        val prefs = activity.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
        if (prefs.getBoolean("auto_open_toolbox", false)) {
            activity.startActivity(Intent(activity, ToolboxActivity::class.java))
        }
    }

    fun attemptAutoMount() {
        if (activity.isWindowsInstalled && 
            !activity.isWindowsMounted && 
            activity.sharedPreferences.getBoolean("automatic_mount_windows", false)
        ) {
            activity.lifecycleScope.launch(Dispatchers.IO) {
                mount()
            }
        }
    }
}