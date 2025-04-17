package id.vern.wincross.helpers

import android.content.Context
import android.graphics.*
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import com.topjohnwu.superuser.Shell
import id.vern.wincross.utils.*
import java.io.File

object UtilityHelper {

  fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    Log.d("UtilityHelper", "Toast: $message")
  }

  fun isWindowsInstalled(context: Context): Boolean {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val path = prefs.getString("Windows", null)
    return !path.isNullOrEmpty()
  }

  fun isWindowsHasBootBackup(context: Context): Boolean {
    val mountPoint = getWindowsMountPoints(context).firstOrNull() ?: return false
    return isFileExists("$mountPoint/boot.img").also {
      Log.d("UtilityHelper", "Windows Boot Backup: ${if (it) "Exists" else "Not Found"}")
    }
  }

  fun isWindowsHasLogoBackup(context: Context): Boolean {
    val mountPoint = getWindowsMountPoints(context).firstOrNull() ?: return false
    return isFileExists("$mountPoint/logo.img").also {
      Log.d("UtilityHelper", "Windows Logo Backup: ${if (it) "Exists" else "Not Found"}")
    }
  }

  private fun getWindowsMountPoints(context: Context): List<String> {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val defaultPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"
    val alternativePath = "${Environment.getExternalStorageDirectory().path}/Windows"

    return when (prefs.getString("Windows Mount Path", defaultPath)) {
      "/mnt/Windows" -> listOf("/mnt/Windows", defaultPath, alternativePath)
      else -> listOf(defaultPath, alternativePath)
    }
  }

  fun isWindowsMounted(context: Context): Boolean {
    for (mountPoint in getWindowsMountPoints(context)) {
      val command = "su -mm -c mount | grep $mountPoint"
      if (Utils.executeShellCommand(command).isSuccess) {
        Log.d("UtilityHelper", "Windows is mounted at: $mountPoint")
        return true
      }
    }
    Log.d("UtilityHelper", "Windows is not mounted.")
    return false
  }

  fun isBackupExists(backupPath: String): Boolean {
    return isFileExists(backupPath).also {
      Log.d("UtilityHelper", "Backup Status: ${if (it) "Exists" else "Not Found"}")
    }
  }

  fun showBlurBackground(rootView: View) =
      rootView.setRenderEffect(RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.MIRROR))

  fun removeBlurBackground(rootView: View) = rootView.setRenderEffect(null)

  fun createFolderIfNotExists(folderPaths: List<String>) {
    folderPaths.forEach { path -> checkAndCreateFolder(path) }
  }

  fun isDeviceRooted(): Boolean {
    val rooted = Shell.isAppGrantedRoot() == true
    Log.d("UtilityHelper", "Device root status: $rooted")
    return rooted
  }

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
