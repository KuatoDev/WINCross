package id.vern.wincross.helpers

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.topjohnwu.superuser.Shell
import id.vern.wincross.R
import android.os.Environment
import java.io.File
import android.view.View

object UtilityHelper {
  /**
   * Displays a toast message to the user and logs it
   * @param context The context to use for showing the toast
   * @param message The message to display
   */
  fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    Log.d("UtilityHelper", "Toast: $message")
  }

  /**
   * Checks if Windows is installed by checking for a saved path in preferences
   * @param context The context to access shared preferences
   * @return true if Windows path exists in preferences, false otherwise
   */
  fun isWindowsInstalled(context: Context): Boolean {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val path = prefs.getString("Windows", null)
    return !path.isNullOrEmpty()
  }

  /**
   * Checks if Windows has a boot backup file
   * @param context The context to determine Windows mount path
   * @return true if boot.img backup exists, false otherwise
   */
  fun isWindowsHasBootBackup(context: Context): Boolean {
    val mountPoint = getWindowsMountPoints(context).firstOrNull() ?: return false
    return isFileExists("$mountPoint/boot.img").also {
      Log.d("UtilityHelper", "Windows Boot Backup: ${if (it) "Exists" else "Not Found"}")
    }
  }

  /**
   * Checks if Windows has a logo backup file
   * @param context The context to determine Windows mount path
   * @return true if logo.img backup exists, false otherwise
   */
  fun isWindowsHasLogoBackup(context: Context): Boolean {
    val mountPoint = getWindowsMountPoints(context).firstOrNull() ?: return false
    return isFileExists("$mountPoint/logo.img").also {
      Log.d("UtilityHelper", "Windows Logo Backup: ${if (it) "Exists" else "Not Found"}")
    }
  }

  /**
   * Checks if Windows partition is currently mounted
   * @param context The context to determine Windows mount paths
   * @return true if Windows is mounted, false otherwise
   */
  fun isWindowsMounted(context: Context): Boolean {
    for (mountPoint in getWindowsMountPoints(context)) {
      val command = "su -mm -c mount | grep $mountPoint"
      if (Shell.cmd(command).exec().isSuccess) {
        Log.d("UtilityHelper", "Windows is mounted at: $mountPoint")
        return true
      }
    }
    Log.d("UtilityHelper", "Windows is not mounted.")
    return false
  }

  /**
   * Returns list of possible Windows mount point paths based on preferences
   * @param context The context to access shared preferences
   * @return List of possible mount paths for Windows
   */
  private fun getWindowsMountPoints(context: Context): List<String> {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val defaultPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"
    val alternativePath = "${Environment.getExternalStorageDirectory().path}/Windows"
    return if (prefs.getBoolean("mount_to_mnt", false)) {
      listOf("/mnt/Windows", defaultPath, alternativePath)
    } else {
      listOf(defaultPath, alternativePath)
    }
  }

  /**
   * Checks if backup file exists at the specified path
   * @param backupPath Path to the backup file
   * @return true if backup exists, false otherwise
   */
  fun isBackupExists(backupPath: String): Boolean {
    return isFileExists(backupPath).also {
      Log.d("UtilityHelper", "Backup Status: ${if (it) "Exists" else "Not Found"}")
    }
  }

  /**
   * Applies blur effect to a view's background
   * @param rootView The view to apply blur effect to
   */
  fun showBlurBackground(rootView: View) =
  rootView.setRenderEffect(RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.MIRROR))

  /**
   * Removes blur effect from a view's background
   * @param rootView The view to remove blur effect from
   */
  fun removeBlurBackground(rootView: View) = rootView.setRenderEffect(null)

  /**
   * Creates multiple folders if they don't exist
   * @param folderPaths List of folder paths to create
   */
  fun createFolderIfNotExists(folderPaths: List<String>) {
    folderPaths.forEach {
      path -> checkAndCreateFolder(path)
    }
  }

  /**
   * Checks if the device has root access
   * @return true if the device is rooted, false otherwise
   */
  fun isDeviceRooted(): Boolean {
    val rooted = Shell.isAppGrantedRoot() == true
    Log.d("UtilityHelper", "Device root status: $rooted")
    return rooted
  }

  /**
   * Checks if a file exists at the specified path
   * @param path Path to the file to check
   * @return true if file exists, false otherwise
   */
  fun isFileExists(path: String): Boolean {
    return try {
      val file = File(path)
      file.exists() && file.isFile
    } catch (e: SecurityException) {
      Log.e("FileCheck", "Security exception when checking file: $path", e)
      false
    } catch (e: Exception) {
      Log.e("FileCheck", "Error checking file: $path", e)
      false
    }
  }

  /**
   * Creates a folder at the specified path if it doesn't exist
   * Sets appropriate permissions on the newly created folder
   * @param path Path to create the folder at
   */
  private fun checkAndCreateFolder(path: String) {
    try {
      val folder = File(path)
      if (!folder.exists()) {
        val success = folder.mkdirs()
        if (success) {
          folder.setReadable(true, false)
          folder.setWritable(true, false)
          folder.setExecutable(true, false)
        } else {
          Log.e("UtilityHelper", "Failed to create folder at $path.")
        }
      }
    } catch (e: Exception) {
      Log.e("UtilityHelper", "Exception when creating folder at $path: ${e.message}")
    }
  }

  /**
   * Checks if UEFI file is available based on stored preferences
   * @param context The context to access shared preferences
   * @return true if UEFI file exists at the stored path, false otherwise
   */
  fun isUefiFileAvailable(context: Context): Boolean {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val path = prefs.getString("UEFI", null)
    return if (path != null) {
      isFileExists(path).also {
        Log.d("UtilityHelper", "UEFI File Status: ${if (it) "Available" else "Not Found"}")
      }
    } else {
      Log.d("UtilityHelper", "UEFI File Status: Not Found (Path is null)")
      false
    }
  }
}