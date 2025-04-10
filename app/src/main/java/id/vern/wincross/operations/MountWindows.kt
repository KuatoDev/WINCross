package id.vern.wincross.operations

import android.content.Context
import android.os.Environment
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.File
import id.vern.wincross.R
import id.vern.wincross.utils.*

/**
 * Object responsible for mounting and unmounting Windows partition
 * Handles NTFS mounting operations with root access
 */
object MountWindows {

  /**
   * Default path for Windows mount point in external storage
   */
  private val kernelInWindows = "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"

  /**
   * Retrieves the Windows partition path from shared preferences
   * @param context The context to access shared preferences
   * @return Path to the Windows partition or null if not set
   */
  fun getWindowsPartitionPath(context: Context): String? {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    return prefs.getString("Windows", null)
  }

  /**
   * Mounts the Windows partition at the specified mount point
   * Uses mount.ntfs executable to mount NTFS partition
   * @param context The context to access shared preferences and files
   * @return true if mounting was successful, false otherwise
   */
  fun mount(context: Context): Boolean {
    val mountNtfsPath = File(context.filesDir, "mount.ntfs").absolutePath
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val mountPoint = if (prefs.getBoolean("mount_to_mnt", false)) "/mnt/Windows" else kernelInWindows
    val partition = getWindowsPartitionPath(context)
    val libraryPath = context.filesDir.absolutePath

    Log.d("MountWindows", "Mounting Windows partition at: $mountPoint")

    if (!File(mountNtfsPath).exists()) {
      Log.e("MountWindows", "ERROR: mount.ntfs file not found.")
      return false
    }

    if (partition == null) {
      Log.e("MountWindows", "ERROR: Windows partition path not found in preferences.")
      return false
    }

    // Create mount point directory and set permissions
    val checkMountCommand = "su -mm -c mkdir -p $mountPoint && chmod 777 $mountPoint"
    if (!Utils.executeShellCommand(checkMountCommand, "MountWindows", logSuccess = true)) {
      Log.e("MountWindows", "Failed to create or set permissions for mount point: $mountPoint")
      return false
    }

    // Execute mount command with library path for NTFS driver
    val mountCommand = "su -mm -c LD_LIBRARY_PATH=$libraryPath $mountNtfsPath $partition $mountPoint"
    val success = Utils.executeShellCommand(mountCommand, "MountWindows", logSuccess = true, logFailure = true)

    if (success) {
      Log.d("MountWindows", "Successfully mounted Windows partition at: $mountPoint")
    } else {
      Log.e("MountWindows", "Failed to mount Windows partition at: $mountPoint")
    }
    return success
  }

  /**
   * Unmounts the Windows partition from the specified mount point
   * Uses the umount command with root privileges
   * @param context The context to access shared preferences
   * @return true if unmounting was successful, false otherwise
   */
  suspend fun umount(context: Context): Boolean {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val mountPoint = if (prefs.getBoolean("mount_to_mnt", false)) "/mnt/Windows" else kernelInWindows

    Log.d("MountWindows", "Attempting to unmount Windows from: $mountPoint")

    val command = "su -mm -c umount $mountPoint"
    val result = Shell.cmd(command).exec()

    return if (result.isSuccess) {
      Log.d("MountWindows", "Successfully unmounted Windows from $mountPoint.")
      true
    } else {
      Log.e("MountWindows", "Failed to unmount Windows from $mountPoint. Error: ${result.err}")
      false
    }
  }
}