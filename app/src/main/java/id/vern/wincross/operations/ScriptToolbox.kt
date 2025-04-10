package id.vern.wincross.operations

import android.content.Context
import android.os.Environment
import android.util.Log
import id.vern.wincross.helpers.DialogHelper
import id.vern.wincross.utils.AssetsManager
import id.vern.wincross.R
import java.io.File
import kotlinx.coroutines.*

object ScriptToolbox {
  private const val TAG = "ScriptToolbox"

  private data class ExtractPaths(
    val toolboxPath: String,
    val desktopPath: String
  )

  private data class FileGroup(
    val files: List<String>,
    val destinationPath: (ExtractPaths) -> String
  )

  private val fileGroups = mapOf(
    "toolbox" to FileGroup(
      files = listOf(
        "usbhostmode.exe",
        "display.exe",
        "Optimized_Taskbar_Control_V3.0.exe"
      ),
      destinationPath = {
        it.toolboxPath
      }
    ),
    "desktop" to FileGroup(
      files = listOf(
        "USB Host Mode.lnk",
        "RotationShortcut.lnk",
        "RotationShortcutReverseLandscape.lnk"
      ),
      destinationPath = {
        it.desktopPath
      }
    )
  )

  fun extractScript(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val paths = getPaths(context)

        // Validate directories
        if (!validateDirectories(paths)) {
          showExtractionResult(context, false)
          return@launch
        }

        // Extract files
        val success = extractFiles(context, paths)
        showExtractionResult(context, success)
      } catch (e: Exception) {
        Log.e(TAG, "Extraction failed: ${e.message}", e)
        showExtractionResult(context, false)
      }
    }
  }

  private fun getPaths(context: Context): ExtractPaths {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val basePath = if (prefs.getBoolean("mount_to_mnt", false)) {
      "/mnt/Windows"
    } else {
      "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"
    }

    return ExtractPaths(
      toolboxPath = "$basePath/Toolbox",
      desktopPath = "$basePath/Users/Public/Desktop"
    )
  }

  private fun validateDirectories(paths: ExtractPaths): Boolean =
  File(paths.toolboxPath).mkdirs() && File(paths.desktopPath).mkdirs()

  private suspend fun extractFiles(context: Context, paths: ExtractPaths): Boolean =
  withContext(Dispatchers.IO) {
    fileGroups.values.all {
      group ->
      group.files.all {
        file ->
        try {
          AssetsManager.copyAssetFile(
            context = context,
            assetFileName = file,
            destinationDir = group.destinationPath(paths)
          )
          Log.d(TAG, "Successfully extracted: $file")
          true
        } catch (e: Exception) {
          Log.e(TAG, "Failed to extract $file: ${e.message}")
          false
        }
      }
    }
  }

  private suspend fun showExtractionResult(context: Context, success: Boolean) {
    withContext(Dispatchers.Main) {
      val messageResId = if (success) {
        R.string.extract_script_success
      } else {
        R.string.extract_failed_general
      }

      DialogHelper.showPopupNotifications(
        context,
        context.getString(messageResId)
      )

      if (success) {
        Log.d(TAG, "Extraction completed successfully")
      } else {
        Log.w(TAG, "Extraction completed with errors")
      }
    }
  }
}