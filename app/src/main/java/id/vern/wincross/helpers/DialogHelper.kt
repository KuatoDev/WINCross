package id.vern.wincross.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import id.vern.wincross.R
import id.vern.wincross.operations.ReviAtlas
import id.vern.wincross.helpers.UEFIHelper
import id.vern.wincross.utils.Utils
import java.io.File

/**
 * Helper object to manage various types of dialogs in the application
 * Created by: KuatoDev
 * Last Updated: 2025-04-06 07:50:41 UTC
 */
object DialogHelper {
  private const val TAG = "DialogHelper"
  private const val DIALOG_DELAY_SHORT = 200L
  private const val DIALOG_DELAY_MEDIUM = 500L

  /**
     * Base function to create a dialog using MaterialAlertDialogBuilder
     *
     * @param context Application context
     * @param rootView View for background blur effect (optional)
     * @param title Dialog title
     * @param message Dialog message
     * @param positiveButtonText Text for positive button (defaults to "Yes")
     * @param negativeButtonText Text for negative button (defaults to "Cancel")
     * @param onPositive Callback when positive button is clicked
     * @param onNegative Callback when negative button is clicked
     * @param onDismiss Callback when dialog is dismissed
     */
  fun createDialog(
    context: Context,
    rootView: View?,
    title: String,
    message: String,
    positiveButtonText: String? = context.getString(R.string.yes),
    negativeButtonText: String? = context.getString(R.string.cancel),
    onPositive: () -> Unit = {},
    onNegative: () -> Unit = {},
    onDismiss: () -> Unit = {}
  ) {
    Log.d(TAG, "createDialog: Displaying dialog with title '$title'")
    rootView?.let {
      UtilityHelper.showBlurBackground(it)
    }

    val builder = MaterialAlertDialogBuilder(context)
    .setTitle(title)
    .setMessage(message)

    positiveButtonText?.let {
      builder.setPositiveButton(it) {
        dialog, _ ->
        Log.d(TAG, "createDialog: Positive button clicked for '$title'")
        onPositive()
        dialog.dismiss()
      }
    }

    negativeButtonText?.let {
      builder.setNegativeButton(it) {
        dialog, _ ->
        Log.d(TAG, "createDialog: Negative button clicked for '$title'")
        onNegative()
        dialog.dismiss()
      }
    }

    val dialog = builder.create()

    dialog.setOnDismissListener {
      Log.d(TAG, "createDialog: Dialog dismissed for '$title'")
      rootView?.let {
        UtilityHelper.removeBlurBackground(it)
      }
      onDismiss()
    }

    dialog.show()
    Log.d(TAG, "createDialog: Dialog shown for '$title'")
  }

  /**
     * Opens file manager to a specific folder
     * Attempts two methods:
     * 1. Direct with Intent.ACTION_VIEW
     * 2. Using FileProvider if first method fails
     */
  private fun openFileManager(context: Context, rootView: View, folderPath: String) {
    val folder = File(folderPath)
    try {
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.fromFile(folder), "resource/folder")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addCategory(Intent.CATEGORY_DEFAULT)
      }

      context.startActivity(intent)
      rootView.let {
        UtilityHelper.removeBlurBackground(it)
      }
      Log.d(TAG, "openFileManager: File manager opened for $folder")
    } catch (e: Exception) {
      Log.e(TAG, "openFileManager: Failed to open primary intent: ${e.message}")
      try {
        val uri = FileProvider.getUriForFile(
          context,
          "${context.packageName}.fileprovider",
          folder
        )
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(uri, "resource/folder")
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(fallbackIntent)
        rootView.let {
          UtilityHelper.removeBlurBackground(it)
        }
        Log.d(TAG, "openFileManager: Opened using FileProvider for $folder")
      } catch (e2: Exception) {
        Log.e(TAG, "openFileManager: All attempts failed: ${e2.message}")
        rootView.let {
          UtilityHelper.removeBlurBackground(it)
        }
        UtilityHelper.showToast(context, "Failed to open file manager.")
      }
    }
  }

  /**
     * Shows confirmation dialog for opening file manager
     * with configurable delay
     */
  private fun showOpenFileManagerDialog(
    context: Context,
    rootView: View,
    title: String,
    message: String,
    folderPath: String,
    delay: Long = DIALOG_DELAY_SHORT
  ) {
    Handler(Looper.getMainLooper()).postDelayed({
      createDialog(
        context = context,
        rootView = rootView,
        title = title,
        message = message,
        positiveButtonText = context.getString(R.string.yes),
        negativeButtonText = context.getString(R.string.no),
        onPositive = {
          openFileManager(context, rootView, folderPath)
        }
      )
    }, delay)
  }

  /**
     * Shows dialog for kernel backup confirmation
     * Opens file manager to backup folder after completion
     */
  fun showBackupKernelPopup(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
      context = context,
      rootView = rootView,
      title = context.getString(R.string.title_backup),
      message = context.getString(R.string.msg_backup_kernel),
      onPositive = {
        onConfirm()
        showOpenFileManagerDialog(
          context = context,
          rootView = rootView,
          title = context.getString(R.string.title_open_file_manager),
          message = context.getString(R.string.msg_open_backup),
          folderPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Backup",
          delay = DIALOG_DELAY_MEDIUM
        )
      }
    )
  }

  /**
     * Shows confirmation dialog for UEFI flashing
     */
  fun showFlashUefiPopup(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
      context = context,
      rootView = rootView,
      title = context.getString(R.string.switch_to_windows),
      message = context.getString(R.string.msg_flash_uefi),
      onPositive = onConfirm
    )
  }

  /**
     * Shows dialog for Windows partition mounting
     * Opens file manager to mount point after successful mount
     */
  fun showMountWindowsPopup(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
      context = context,
      rootView = rootView,
      title = context.getString(R.string.title_mount_windows),
      message = context.getString(R.string.msg_mount_windows),
      onPositive = {
        onConfirm()
        val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
        val mountPoint = if (prefs.getBoolean("mount_to_mnt", false))
          "/mnt/Windows"
        else
          "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"

        showOpenFileManagerDialog(
          context = context,
          rootView = rootView,
          title = context.getString(R.string.title_open_file_manager),
          message = context.getString(R.string.msg_open_file_manager),
          folderPath = mountPoint
        )
      }
    )
  }

  /**
     * Shows confirmation dialog for Windows partition unmounting
     */
  fun showUmountWindowsPopup(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
      context = context,
      rootView = rootView,
      title = context.getString(R.string.title_umount_windows),
      message = context.getString(R.string.msg_umount_windows),
      onPositive = onConfirm
    )
  }

  /**
     * Shows dialog for STA (State Awareness) creation
     */
  fun showSTACreator(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
      context = context,
      rootView = rootView,
      title = context.getString(R.string.title_create_sta),
      message = context.getString(R.string.msg_create_sta),
      onPositive = onConfirm
    )
  }

  /**
     * Shows confirmation dialog for ARM software installation
     */
  fun showARMSoftware(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
      context = context,
      rootView = rootView,
      title = context.getString(R.string.title_arm_software),
      message = context.getString(R.string.msg_arm_software),
      onPositive = onConfirm
    )
  }

  /**
     * Shows dialog for script toolbox options
     */
  fun showScriptToolbox(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
      context = context,
      rootView = rootView,
      title = context.getString(R.string.title_script),
      message = context.getString(R.string.msg_script),
      onPositive = onConfirm
    )
  }

  /**
     * Shows confirmation dialog for frameworks download
     */
  fun showDownloadFrameworks(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
      context = context,
      rootView = rootView,
      title = context.getString(R.string.title_download_frameworks),
      message = context.getString(R.string.msg_download_frameworks),
      onPositive = onConfirm
    )
  }

  /**
     * Shows OS selection dialog (Atlas OS or Revi OS)
     * Initiates download based on selection
     */
  fun showReviAtlas(context: Context, rootView: View) {
    createDialog(
      context = context,
      rootView = rootView,
      title = "Download Playbook",
      message = "Choose OS to download:",
      positiveButtonText = "Atlas OS",
      negativeButtonText = "Revi OS",
      onPositive = {
        UtilityHelper.showToast(context, "Downloading Atlas OS Playbook...")
        ReviAtlas.downloadReviAtlas(context, "AtlasPlaybook.apbx", "AtlasOS")
      },
      onNegative = {
        UtilityHelper.showToast(context, "Downloading Revi OS Playbook...")
        ReviAtlas.downloadReviAtlas(context, "ReviPlaybook.apbx", "ReviOS")
      }
    )
  }

  /**
     * Shows generic confirmation dialog
     * Can be used for various purposes
     */
  fun showConfirmationDialog(
    context: Context,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {}
  ) {
    createDialog(
      context = context,
      rootView = null,
      title = title,
      message = message,
      onPositive = onConfirm,
      onNegative = onCancel
    )
  }

  /**
     * Shows simple notification dialog without buttons
     */
  fun showPopupNotifications(context: Context, msg: String) {
    val safeActivity = context as? Activity ?: return
    val rootView = safeActivity.window?.decorView?.rootView

    createDialog(
      context = context,
      rootView = rootView,
      title = "",
      message = msg,
      positiveButtonText = null,
      negativeButtonText = null
    )
  }

  /**
     * Shows UEFI management dialog
     * Options to select or clear UEFI with optional callback
     */
  fun showUEFIOptionsDialog(context: Context, rootView: View, callback: (() -> Unit)? = null) {
  createDialog(
    context = context,
    rootView = rootView,
    title = context.getString(R.string.select_uefi),
    message = context.getString(R.string.select_uefi_file),
    positiveButtonText = context.getString(R.string.select_uefi),
    negativeButtonText = context.getString(R.string.clear_uefi),
    onPositive = {
      UEFIHelper.selectUEFIFile()
      callback?.invoke()
    },
    onNegative = {
      UEFIHelper.clearSavedUEFI()
      callback?.invoke()
    },
    onDismiss = {
      callback?.invoke()
    }
  )
}

/**
     * Shows reboot confirmation dialog
     * Different messages based on flash status
     * Handles reboot with permission check
     */
fun showRebootDialog(context: Context, flashSuccessful: Boolean) {
  val titleRes = if (flashSuccessful) R.string.reboot_message else R.string.reboot_message_warning
  val messageRes = if (flashSuccessful) R.string.reboot_message else R.string.reboot_message_warning

  createDialog(
    context = context,
    rootView = (context as? Activity)?.window?.decorView?.rootView,
    title = context.getString(titleRes),
    message = context.getString(messageRes),
    positiveButtonText = context.getString(R.string.yes),
    negativeButtonText = context.getString(R.string.no),
    onPositive = {
      try {
        if (ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.REBOOT
        ) == PackageManager.PERMISSION_GRANTED
        ) {
          val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
          powerManager.reboot(null)
        } else {
          Utils.executeShellCommand("su -mm -c reboot")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to reboot: ${e.message}")
        UtilityHelper.showToast(context, "Failed to reboot: ${e.message}")
      }
    }
  )
}

/**
     * Shows update available dialog with download option
     */
fun showUpdateAvailableDialog(
  context: Context,
  rootView: View,
  currentVersion: String,
  newVersion: String,
  downloadUrl: String
) {
  // Show blur background
  rootView.let {
    UtilityHelper.showBlurBackground(it)
  }

  MaterialAlertDialogBuilder(context).apply {
    setTitle("Update Available")
    setIcon(R.drawable.ic_update)
    setMessage(
      """
                A new version of UEFI is available!
                Current version: v$currentVersion
                Latest version: v$newVersion
                Would you like to download the update now?
                """.trimIndent()
    )
    setPositiveButton("Download") {
      dialog, _ ->
      try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
          data = Uri.parse(downloadUrl)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
      } catch (e: Exception) {
        UtilityHelper.showToast(context, "Failed to open download link")
      }
      // Remove blur on download
      rootView.let {
        UtilityHelper.removeBlurBackground(it)
      }
      dialog.dismiss()
    }
    setNegativeButton("Later") {
      dialog, _ ->
      // Remove blur on later
      rootView.let {
        UtilityHelper.removeBlurBackground(it)
      }
      dialog.dismiss()
    }
    setCancelable(false)

    // Create and show dialog
    val dialog = create()

    // Remove blur on dismiss
    dialog.setOnDismissListener {
      rootView.let {
        UtilityHelper.removeBlurBackground(it)
      }
    }

    dialog.show()
  }
}
}