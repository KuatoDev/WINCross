package id.vern.wincross.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import id.vern.wincross.activity.*
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.*
import id.vern.wincross.R
import kotlinx.coroutines.launch

class UefiUpdateManager(private val activity: MainActivity) {
    private var isShowingUpdateDialog = false
    private val prefs = activity.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    private data class DeviceUpdate(val owner: String, val repo: String)

    fun checkForUpdates() {
        if (!shouldCheckUpdate()) return

        val deviceModel = prefs.getString("device_model", null) 
            ?: Utils.getDeviceModel(activity)

        getDeviceUpdate(deviceModel)?.let { (owner, repo) ->
            startUpdateCheck(owner, repo)
        }
    }

    private fun shouldCheckUpdate(): Boolean {
        return prefs.getBoolean("uefi_auto_update", false) 
            && !isShowingUpdateDialog
    }

    private fun getDeviceUpdate(deviceModel: String): DeviceUpdate? = when {
        deviceModel.contains("vayu", true) -> 
            DeviceUpdate("woa-vayu", "POCOX3Pro-Releases")
        deviceModel.contains("nabu", true) -> 
            DeviceUpdate("erdilS", "Port-Windows-11-Xiaomi-Pad-5")
        deviceModel.contains("a52s", true) -> 
            DeviceUpdate("woa-a52s", "Samsung-A52s-5G-Releases")
        deviceModel.contains("beryllium", true) -> 
            DeviceUpdate("n00b69", "woa-beryllium")
        deviceModel.contains("cepheus", true) -> 
            DeviceUpdate("qaz6750", "XiaoMi9-Drivers")
        else -> null
    }

    private fun startUpdateCheck(owner: String, repo: String) {
        isShowingUpdateDialog = true
        activity.lifecycleScope.launch {
            try {
                val (hasUpdate, latestVersion, downloadUrl) = 
                    UEFIHelper.checkForUpdates(owner, repo)

                if (hasUpdate && downloadUrl.isNotEmpty()) {
                    showUpdateDialog(latestVersion, downloadUrl)
                }
            } catch (e: Exception) {
                UtilityHelper.showToast(activity, 
                    "Failed to check for updates: ${e.message}")
            }
            isShowingUpdateDialog = false
        }
    }

    private fun showUpdateDialog(latestVersion: String, downloadUrl: String) {
    if (activity.isFinishing || activity.isDestroyed) return

    DialogHelper.createDialog(
        context = activity,
        rootView = activity.window.decorView.rootView,
        title = activity.getString(R.string.update_dialog_title),
        message = activity.getString(
            R.string.update_dialog_message,
            UEFIHelper.getCurrentVersion(),
            latestVersion
        ),
        positiveButtonText = activity.getString(R.string.update_dialog_positive),
        negativeButtonText = activity.getString(R.string.update_dialog_negative),
        onPositive = { openDownloadUrl(downloadUrl) },
        onDismiss = { isShowingUpdateDialog = false }
    )
}

    private fun openDownloadUrl(downloadUrl: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(downloadUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            UtilityHelper.showToast(activity, "Failed to open download link")
        }
    }
}