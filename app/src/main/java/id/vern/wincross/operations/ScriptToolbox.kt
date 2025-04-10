package id.vern.wincross.operations

import android.content.Context
import android.os.Environment
import android.util.Log
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.*
import id.vern.wincross.R
import java.io.File
import kotlinx.coroutines.*

object ScriptToolbox {
  private const val TAG = "ScriptToolbox"
  private const val PREFS_NAME = "WinCross_preferences"
  private const val PREF_MOUNT_TO_MNT = "mount_to_mnt"

  private val toolboxFiles = listOf(
    "usbhostmode.exe",
    "display.exe",
    "Optimized_Taskbar_Control_V3.0.exe"
  )

  private val desktopFiles = listOf(
    "USB Host Mode.lnk",
    "RotationShortcut.lnk",
    "RotationShortcutReverseLandscape.lnk"
  )

  private data class ExtractPaths(
    val toolboxPath: String,
    val desktopPath: String
  )

  fun extractScript(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val paths = getPaths(context)

        // Validate directories
        if (!validateDirectories(paths)) {
          showExtractionResult(context, false, R.string.extract_failed_general)
          return@launch
        }

        // Extract files
        val result = extractFiles(context, paths)
        handleExtractionResult(context, result)
      } catch (e: Exception) {
        Log.e(TAG, "Extraction failed: ${e.message}", e)
        showExtractionResult(context, false, R.string.extract_failed_general)
      }
    }
  }

  private fun getPaths(context: Context) = ExtractPaths(
    toolboxPath = if (context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(PREF_MOUNT_TO_MNT, false)
    ) {
      "/mnt/Windows/Toolbox"
    } else {
      "${Environment.getExternalStorageDirectory().path}/WINCross/Windows/Toolbox"
    },
    desktopPath = if (context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(PREF_MOUNT_TO_MNT, false)
    ) {
      "/mnt/Windows/Users/Public/Desktop"
    } else {
      "${Environment.getExternalStorageDirectory().path}/WINCross/Windows/Users/Public/Desktop"
    }
  )

  private fun validateDirectories(paths: ExtractPaths): Boolean {
    return File(paths.toolboxPath).mkdirs() && File(paths.desktopPath).mkdirs()
  }

  private suspend fun extractFiles(context: Context, paths: ExtractPaths): Boolean {
    var success = true

    withContext(Dispatchers.IO) {
      // Extract toolbox files
      toolboxFiles.forEach {
        file ->
        try {
          AssetsManager.copyAssetFile(context, file, paths.toolboxPath)
          Log.d(TAG, "Successfully extracted: $file")
        } catch (e: Exception) {
          Log.e(TAG, "Failed to extract $file: ${e.message}")
          success = false
        }
      }

      // Extract desktop files
      desktopFiles.forEach {
        file ->
        try {
          AssetsManager.copyAssetFile(context, file, paths.desktopPath)
          Log.d(TAG, "Successfully extracted: $file")
        } catch (e: Exception) {
          Log.e(TAG, "Failed to extract $file: ${e.message}")
          success = false
        }
      }
    }

    return success
  }

  private suspend fun handleExtractionResult(context: Context, success: Boolean) {
    val messageResId = if (success) {
      R.string.extract_script_success
    } else {
      R.string.extract_failed_general
    }
    showExtractionResult(context, success, messageResId)
  }

  private suspend fun showExtractionResult(
    context: Context,
    success: Boolean,
    messageResId: Int
  ) {
    withContext(Dispatchers.Main) {
      DialogHelper.showPopupNotifications(context, context.getString(messageResId))
      if (success) {
        Log.d(TAG, "Extraction completed successfully")
      } else {
        Log.w(TAG, "Extraction completed with errors")
      }
    }
  }
}