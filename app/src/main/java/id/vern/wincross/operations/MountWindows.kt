package id.vern.wincross.operations

import android.content.Context
import android.util.Log
import id.vern.wincross.utils.*
import java.io.File

object MountWindows {
  private const val TAG = "MountWindows"
  private const val PREFS_NAME = "WinCross_preferences"
  private const val PREF_WINDOWS = "Windows"
  private const val PREF_MOUNT_TO_MNT = "Windows Mount Path"

  fun getWindowsPartitionPath(context: Context): String? {
    return context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREF_WINDOWS, null)
  }

  fun mount(context: Context): Boolean {
    val mountNtfsPath = File(context.filesDir, "mount.ntfs").absolutePath
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val mountPoint = prefs.getString(PREF_MOUNT_TO_MNT, null)
    val partition = getWindowsPartitionPath(context)
    val libraryPath = context.filesDir.absolutePath

    Log.d(TAG, "Mounting Windows partition at: $mountPoint")

    if (!File(mountNtfsPath).exists()) {
      Log.e(TAG, "ERROR: mount.ntfs file not found.")
      return false
    }

    if (partition == null) {
      Log.e(TAG, "ERROR: Windows partition path not found in preferences.")
      return false
    }

    // Create mount point directory and set permissions
    val checkMountCommand = "su -mm -c mkdir -p $mountPoint && chmod 777 $mountPoint"
    if (!Utils.executeShellCommand(checkMountCommand, TAG, logSuccess = true).isSuccess) {
      Log.e(TAG, "Failed to create or set permissions for mount point: $mountPoint")
      return false
    }

    // Execute mount command with library path for NTFS driver
    val mountCommand =
        "su -mm -c LD_LIBRARY_PATH=$libraryPath $mountNtfsPath $partition $mountPoint"
    val result = Utils.executeShellCommand(mountCommand, TAG, logSuccess = true, logFailure = true)

    return result.isSuccess.also { success ->
      if (success) {
        Log.d(TAG, "Successfully mounted Windows partition at: $mountPoint")
      } else {
        Log.e(TAG, "Failed to mount Windows partition at: $mountPoint")
      }
    }
  }

  suspend fun umount(context: Context): Boolean {
    val mountPoint =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_MOUNT_TO_MNT, null)

    Log.d(TAG, "Attempting to unmount Windows from: $mountPoint")

    val command = "su -mm -c umount $mountPoint"
    val result = Utils.executeShellCommand(command)

    return if (result.isSuccess) {
      Log.d(TAG, "Successfully unmounted Windows from $mountPoint.")
      true
    } else {
      Log.e(TAG, "Failed to unmount Windows from $mountPoint. Error: ${result.out}")
      false
    }
  }
}
