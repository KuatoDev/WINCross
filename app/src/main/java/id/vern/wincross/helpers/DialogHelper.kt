package id.vern.wincross.helpers

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.view.View
import android.widget.*
import androidx.core.content.*
import androidx.preference.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import id.vern.wincross.R
import id.vern.wincross.operations.ReviAtlas
import id.vern.wincross.utils.Utils
import java.io.File

object DialogHelper {
  private const val TAG = "DialogHelper"
  private const val DIALOG_DELAY_SHORT = 200L
  private const val DIALOG_DELAY_MEDIUM = 500L

  fun createDialog(
      context: Context,
      rootView: View?,
      title: String,
      message: String,
      positiveButtonText: String? = context.getString(R.string.yes),
      negativeButtonText: String? = context.getString(R.string.cancel),
      onPositive: (() -> Unit)? = null,
      onNegative: (() -> Unit)? = null,
      onDismiss: () -> Unit = {}
  ) {
    Log.d(TAG, "createDialog: Displaying dialog with title '$title'")

    rootView?.let {
      UtilityHelper.showBlurBackground(it)
      Log.d(TAG, "createDialog: Blur background applied.")
    }

    val titleView = createDialogTitle(context, title)
    val msgView = createDialogMessage(context, message)
    val buttonView = createDialogButtons(context)

    val container =
        LinearLayout(context).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(0, 0, 0, 0)
          addView(titleView)
          addView(msgView)
          addView(buttonView)
        }

    val builder = MaterialAlertDialogBuilder(context).setView(container)

    val dialog = builder.create()

    val positiveBtn = buttonView.findViewById<MaterialButton>(R.id.positiveButton)
    val negativeBtn = buttonView.findViewById<MaterialButton>(R.id.negativeButton)

    if (onPositive != null) {
      positiveBtn?.apply {
        text = positiveButtonText ?: context.getString(R.string.yes)
        setOnClickListener {
          onPositive()
          rootView?.let {
            UtilityHelper.removeBlurBackground(it)
            Log.d(TAG, "createDialog: Blur background removed (Positive).")
          }
          dialog.dismiss()
        }
        visibility = View.VISIBLE
      }
    } else {
      positiveBtn?.visibility = View.GONE
    }

    if (onNegative != null) {
      negativeBtn?.apply {
        text = negativeButtonText ?: context.getString(R.string.cancel)
        setOnClickListener {
          onNegative()
          rootView?.let {
            UtilityHelper.removeBlurBackground(it)
            Log.d(TAG, "createDialog: Blur background removed (Negative).")
          }
          dialog.dismiss()
        }
        visibility = View.VISIBLE
      }
    } else {
      negativeBtn?.visibility = View.GONE
    }

    dialog.setOnDismissListener {
      Log.d(TAG, "createDialog: Dialog dismissed for '$title'")
      rootView?.let {
        UtilityHelper.removeBlurBackground(it)
        Log.d(TAG, "createDialog: Blur background removed.")
      }
      onDismiss()
    }

    dialog.show()
    Log.d(TAG, "createDialog: Dialog shown for '$title'")
  }

  private fun createDialogTitle(context: Context, title: String): TextView {
    val inflater = LayoutInflater.from(context)
    return (inflater.inflate(R.layout.dialog_title, null) as TextView).apply { text = title }
  }

  private fun createDialogMessage(context: Context, msg: String): TextView {
    val inflater = LayoutInflater.from(context)
    return (inflater.inflate(R.layout.dialog_message, null) as TextView).apply { text = msg }
  }

  private fun createDialogButtons(context: Context): View {
    val inflater = LayoutInflater.from(context)
    return inflater.inflate(R.layout.dialog_button, null)
  }

  private fun openFileManager(context: Context, rootView: View, folderPath: String) {
    val folder = File(folderPath)
    try {
      val intent =
          Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(folder), "resource/folder")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addCategory(Intent.CATEGORY_DEFAULT)
          }

      context.startActivity(intent)
      rootView.let { UtilityHelper.removeBlurBackground(it) }
      Log.d(TAG, "openFileManager: File manager opened for $folder")
    } catch (e: Exception) {
      Log.e(TAG, "openFileManager: Failed to open primary intent: ${e.message}")
      try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", folder)
        val fallbackIntent =
            Intent(Intent.ACTION_VIEW).apply {
              setDataAndType(uri, "resource/folder")
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(fallbackIntent)
        rootView.let { UtilityHelper.removeBlurBackground(it) }
        Log.d(TAG, "openFileManager: Opened using FileProvider for $folder")
      } catch (e2: Exception) {
        Log.e(TAG, "openFileManager: All attempts failed: ${e2.message}")
        rootView.let { UtilityHelper.removeBlurBackground(it) }
        UtilityHelper.showToast(context, "Failed to open file manager.")
      }
    }
  }

  private fun showOpenFileManagerDialog(
      context: Context,
      rootView: View,
      title: String,
      message: String,
      folderPath: String,
      delay: Long = DIALOG_DELAY_SHORT
  ) {
    Handler(Looper.getMainLooper())
        .postDelayed(
            {
              createDialog(
                  context = context,
                  rootView = rootView,
                  title = title,
                  message = message,
                  positiveButtonText = context.getString(R.string.yes),
                  negativeButtonText = context.getString(R.string.no),
                  onPositive = { openFileManager(context, rootView, folderPath) })
            },
            delay)
  }

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
              delay = DIALOG_DELAY_MEDIUM)
        })
  }

  fun showFlashUefiPopup(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = context.getString(R.string.switch_to_windows),
        message = context.getString(R.string.msg_flash_uefi),
        onPositive = onConfirm)
  }

  fun showMountWindowsPopup(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = context.getString(R.string.title_mount_windows),
        message = context.getString(R.string.msg_mount_windows),
        onPositive = {
          onConfirm()
          val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)

          val mountPoint =
              when (prefs.getString("Windows Mount Path", null)) {
                "/mnt/Windows" -> "/mnt/Windows"
                else -> "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"
              }

          showOpenFileManagerDialog(
              context = context,
              rootView = rootView,
              title = context.getString(R.string.title_open_file_manager),
              message = context.getString(R.string.msg_open_file_manager),
              folderPath = mountPoint)
        })
  }

  fun showUmountWindowsPopup(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = context.getString(R.string.title_umount_windows),
        message = context.getString(R.string.msg_umount_windows),
        onPositive = onConfirm)
  }

  fun showSTACreator(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = context.getString(R.string.title_create_sta),
        message = context.getString(R.string.msg_create_sta),
        onPositive = onConfirm)
  }

  fun showARMSoftware(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = context.getString(R.string.title_arm_software),
        message = context.getString(R.string.msg_arm_software),
        onPositive = onConfirm)
  }

  fun showUsbHostmode(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = "USB Host Mode",
        message = "Download USB Host Mode for your Windows?",
        onPositive = onConfirm)
  }

  fun showTaskbarControl(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = "Optimized Taskbar Control",
        message = "Download Optimized Taskbar Control for your Windows?",
        onPositive = onConfirm)
  }

  fun showBootAutoflasher(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = "Boot Autoflasher",
        message = "Download Boot Autoflasher for your Windows?",
        onPositive = onConfirm)
  }

  fun showRotationToggle(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = context.getString(R.string.title_rotation_toggle),
        message = context.getString(R.string.msg_rotation_toggle),
        onPositive = onConfirm)
  }

  fun showDownloadFrameworks(context: Context, rootView: View, onConfirm: () -> Unit) {
    createDialog(
        context = context,
        rootView = rootView,
        title = context.getString(R.string.title_download_frameworks),
        message = context.getString(R.string.msg_download_frameworks),
        onPositive = onConfirm)
  }

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
        })
  }

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
        onNegative = onCancel)
  }

  fun showPopupNotifications(context: Context, msg: String) {
    val safeActivity = context as? Activity ?: return
    val rootView = safeActivity.window?.decorView?.rootView
    createDialog(
        context = context,
        rootView = rootView,
        title = "Notifications",
        message = msg,
        positiveButtonText = null,
        negativeButtonText = null)
  }

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
        onDismiss = { callback?.invoke() })
  }

  fun showRebootDialog(context: Context, flashSuccessful: Boolean) {
    val titleRes =
        if (flashSuccessful) R.string.switch_to_windows else R.string.reboot_message_warning
    val messageRes =
        if (flashSuccessful) R.string.reboot_message else R.string.reboot_message_warning

    createDialog(
        context = context,
        rootView = (context as? Activity)?.window?.decorView?.rootView,
        title = context.getString(titleRes),
        message = context.getString(messageRes),
        positiveButtonText = context.getString(R.string.yes),
        negativeButtonText = context.getString(R.string.no),
        onPositive = {
          try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.REBOOT) ==
                PackageManager.PERMISSION_GRANTED) {
              val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
              powerManager.reboot(null)
            } else {
              Utils.executeShellCommand("su -mm -c reboot")
            }
          } catch (e: Exception) {
            Log.e(TAG, "Failed to reboot: ${e.message}")
            UtilityHelper.showToast(context, "Failed to reboot: ${e.message}")
          }
        })
  }
}
