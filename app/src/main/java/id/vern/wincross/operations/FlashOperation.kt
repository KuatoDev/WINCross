package id.vern.wincross.operations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import android.app.Activity
import kotlin.coroutines.resume

object FlashOperation {
  private val logoFolderPath = "${Environment.getExternalStorageDirectory().path}/WINCross/UEFI"
  private val backupPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Backup"
  private val kernelInWindows = "${Environment.getExternalStorageDirectory().path}/WINCross/Windows/"
  private const val PREFERENCES_NAME = "WinCross_preferences"
  private const val BLOCK_SIZE = "8M"
  private const val TAG = "FlashOperation"

  fun flashIt(context: Context) {
    val coroutineScope = CoroutineScope(Dispatchers.Main)

    coroutineScope.launch {
      // Step 1: Check if device is rooted
      if (UtilityHelper.isDeviceRooted().not()) {
        UtilityHelper.showToast(context, context.getString(R.string.flash_not_rooted))
        Log.e(TAG, "Cannot flash - device not rooted")
        return@launch
      }

      // Step 1.5: Check if Windows is mounted, if not mount it
      if (!UtilityHelper.isWindowsMounted(context)) {
        Log.d(TAG, "Windows is not mounted, attempting to mount...")
        val mountSuccess = MountWindows.mount(context)
        if (!mountSuccess) {
          UtilityHelper.showToast(context, "Failed to mount Windows")
          Log.e(TAG, "Failed to mount Windows")
          return@launch
        }
        Log.d(TAG, "Windows mounted successfully")
      } else {
        Log.d(TAG, "Windows is already mounted, proceeding...")
      }

      // Step 2: Get user preferences
      val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      val selectedUefiPath = prefs.getString("UEFI", null)

      // Step 3: Check if UEFI path is set
      if (selectedUefiPath.isNullOrBlank()) {
        Log.w(TAG, "No UEFI path selected in preferences")

        val dialogResult = suspendCancellableCoroutine<Boolean> {
          continuation ->
          if (context !is Activity) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
          }

          val rootView = context.window?.decorView?.rootView ?: run {
            continuation.resume(false)
            return@suspendCancellableCoroutine
          }

          DialogHelper.createDialog(
            context = context,
            rootView = rootView,
            title = "UEFI Not Selected",
            message = "Please select a UEFI file to continue",
            positiveButtonText = "Select UEFI",
            negativeButtonText = "Cancel",
            onPositive = {
              UEFIHelper.initialize(context)
              UEFIHelper.selectUEFIFile()
              continuation.resume(true)
            },
            onNegative = {
              continuation.resume(false)
            }
          )
        }

        if (!dialogResult) {
          Log.i(TAG, "User canceled UEFI selection")
          return@launch
        }
        // Wait for file selection to complete
        delay(500)

        // Recheck UEFI selection after dialog
        val updatedUefiPath = prefs.getString("UEFI", null)
        if (updatedUefiPath.isNullOrBlank()) {
          UtilityHelper.showToast(context, context.getString(R.string.flash_missing_files))
          Log.e(TAG, "No UEFI file selected")
          return@launch
        }
      }

      // Step 4: Check if selected UEFI file exists
      val uefiImagePath = prefs.getString("UEFI", "") ?: ""
      if (UtilityHelper.isFileExists(uefiImagePath).not()) {
        Log.e(TAG, "UEFI file not found at: $uefiImagePath")
        UtilityHelper.showToast(context, context.getString(R.string.flash_missing_files))
        return@launch
      }

      // Step 5: Process backup preferences
      val backupIfEmpty = prefs.getBoolean("backup_boot_if_empty", false)
      val forceBackup = prefs.getBoolean("force_backup_to_win", false)
      val alwaysProvisionModem = prefs.getBoolean("always_provision_modem", false)

      // Step 6: Backup boot if needed
      if (backupIfEmpty || forceBackup) {
        val existingBootImgInWindows = File("$kernelInWindows/boot.img")
        val fileExists = existingBootImgInWindows.exists()
        if (forceBackup || !fileExists) {
          val backupSuccess = withContext(Dispatchers.IO) {
            BackupOperation.backupBootToWindows(context)
          }

          if (backupSuccess == false) {
            // This is the safest way to check
            Log.e(TAG, "Failed to backup boot partition")
            UtilityHelper.showToast(context, "Failed to backup boot partition")
          }
        }
      }

      // Step 7: Provision modem if needed
      if (alwaysProvisionModem) {
        val provisionSuccess = withContext(Dispatchers.IO) {
          try {
            ProvisionModem.provision(context)
            true
          } catch (e: Exception) {
            Log.e(TAG, "Exception during modem provisioning: ${e.message}")
            false
          }
        }

        if (!provisionSuccess) {
          Log.e(TAG, "Failed to provision modem")
          // You may want to inform user or handle this situation
        }
      }

      // Step 8: Check if logo flashing is enabled and file exists
      val flashLogoWithUefi = prefs.getBoolean("flash_logo_with_uefi", false)
      val winlogoImagePath = "$logoFolderPath/winlogo.img"

      if (flashLogoWithUefi && UtilityHelper.isFileExists(winlogoImagePath).not()) {
        UtilityHelper.showToast(context, context.getString(R.string.flash_missing_files))
        Log.e(TAG, "Missing logo file for quick boot: winlogo.img")
        return@launch
      }

      // Step 9: Get boot partition path based on active slot
      val activeSlot = prefs.getString("Active Slot", null)
      val bootPath = when (activeSlot) {
        "a_only" -> BackupOperation.getBootPartitionPath(context)
        "slot_a" -> BackupOperation.getBootAPartitionPath(context)
        "slot_b" -> BackupOperation.getBootBPartitionPath(context)
        else -> null
      }

      if (bootPath == null) {
        UtilityHelper.showToast(context, context.getString(R.string.flash_partition_not_found))
        Log.e(TAG, "Boot partition path missing for slot: $activeSlot")
        return@launch
      }

      // Step 10: Get logo partition path if needed
      val logoPath = if (flashLogoWithUefi) {
        BackupOperation.getLogoPartitionPath(context)
      } else {
        null
      }

      if (flashLogoWithUefi && logoPath == null) {
        UtilityHelper.showToast(context, context.getString(R.string.flash_partition_not_found))
        Log.e(TAG, "Logo partition path not found")
        return@launch
      }

      // Step 11: Prepare flash tasks
      val flashTasks = mutableListOf(uefiImagePath to bootPath)
      if (flashLogoWithUefi && logoPath != null) {
        flashTasks.add(winlogoImagePath to logoPath)
      }

      // Step 12: Execute flash tasks sequentially
      var allSuccessful = true
      for ((src, dest) in flashTasks) {
        val result = withContext(Dispatchers.IO) {
          Utils.executeShellCommand("su -mm -c dd if=$src of=$dest bs=$BLOCK_SIZE conv=fsync")
        }

        if (result.not()) {
          allSuccessful = false
          Log.e(TAG, "Failed to flash from $src to $dest")

          if (src == uefiImagePath) {
            UtilityHelper.showToast(context, context.getString(R.string.flash_boot_failed))
            return@launch
          }
        }
      }

      // Step 13: Unmount Windows
      val unmountSuccess = withContext(Dispatchers.IO) {
        try {
          MountWindows.umount(context)
          true
        } catch (e: Exception) {
          Log.e(TAG, "Exception during unmount: ${e.message}")
          false
        }
      }

      if (unmountSuccess.not()) {
        Log.e(TAG, "Failed to unmount Windows")
        // You may want to inform user or handle this situation
      }

      // Step 14: Show reboot dialog
      DialogHelper.showRebootDialog(context, allSuccessful)
    }
  }
}