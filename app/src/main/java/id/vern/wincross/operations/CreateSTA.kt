package id.vern.wincross.operations

import android.content.Context
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.DialogHelper
import id.vern.wincross.managers.*
import java.io.File
import kotlinx.coroutines.*

object CreateSTA {
  private const val TAG = "CreateSTA"

  private fun getRequiredDirectories(windowsPath: String) =
      mapOf(
          "programDataSta" to "$windowsPath/ProgramData/sta",
          "sta" to "$windowsPath/sta",
          "desktop" to "$windowsPath/Users/Public/Desktop")

  private fun getFileExtractionMap(directories: Map<String, String>) =
      mapOf(
          "Switch to Android.lnk" to directories["desktop"]!!,
          "sta.exe" to directories["programDataSta"]!!,
          "sta.exe" to directories["sta"]!!,
          "sdd.exe" to directories["sta"]!!,
          "sdd.conf" to directories["sta"]!!,
          "boot_img_auto-flasher_V1.1.exe" to directories["sta"]!!)

  suspend fun RunSTAAsync(context: Context) =
      withContext(Dispatchers.IO) {
        try {
          val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
          val windowsPath = prefs.getString("Windows Mount Path", null)

          if (windowsPath.isNullOrEmpty()) {
            Log.e(TAG, "Windows path not configured")
            showError(context, R.string.error_modem_provision_failed)
            return@withContext
          }

          val directories = getRequiredDirectories(windowsPath)

          if (!createRequiredDirectories(directories.values.toList())) {
            Log.e(TAG, "Failed to create required directories")
            showError(context, R.string.sta_creation_failed)
            return@withContext
          }

          if (!extractRequiredAssets(context, getFileExtractionMap(directories))) {
            Log.e(TAG, "Failed to extract required assets")
            showError(context, R.string.sta_creation_failed)
            return@withContext
          }

          withContext(Dispatchers.Main) {
            DialogHelper.showPopupNotifications(context, "STA Created in Windows")
          }
          Log.d(TAG, "STA environment created successfully")
        } catch (e: Exception) {
          Log.e(TAG, "Error creating STA environment: ${e.message}", e)
          showError(context, R.string.sta_creation_failed)
        }
      }

  private fun createRequiredDirectories(directoryPaths: List<String>): Boolean {
    return directoryPaths.all { path ->
      val dir = File(path)
      if (dir.exists()) {
        Log.d(TAG, "Directory already exists: $path")
        true
      } else {
        val created = dir.mkdirs()
        if (!created) {
          Log.e(TAG, "Failed to create directory: $path")
        } else {
          Log.d(TAG, "Created directory: $path")
        }
        created
      }
    }
  }

  private fun extractRequiredAssets(
      context: Context,
      filesToExtract: Map<String, String>
  ): Boolean {
    var allSuccessful = true

    filesToExtract.forEach { (assetFile, destinationPath) ->
      try {
        val destFile = File(destinationPath, assetFile)

        if (destFile.exists()) {
          Log.d(TAG, "File already exists, skipping: ${destFile.absolutePath}")
          return@forEach
        }

        AssetsManager.copyAssetFile(context, assetFile, destinationPath)
        Log.d(TAG, "Successfully extracted: $assetFile to $destinationPath")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to extract $assetFile to $destinationPath: ${e.message}", e)
        allSuccessful = false
      }
    }

    return allSuccessful
  }

  private suspend fun showError(context: Context, messageResId: Int) {
    withContext(Dispatchers.Main) {
      DialogHelper.showPopupNotifications(context, context.getString(messageResId))
    }
  }

  @Deprecated("Use RunSTAAsync for better performance and error handling")
  fun RunSTA(context: Context) {
    CoroutineScope(Dispatchers.IO).launch { RunSTAAsync(context) }
  }
}
