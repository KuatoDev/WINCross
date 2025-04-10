package id.vern.wincross.operations

import android.content.Context
import android.os.Environment
import android.util.Log
import id.vern.wincross.helpers.DialogHelper
import id.vern.wincross.utils.AssetsManager
import kotlinx.coroutines.*
import java.io.File
import id.vern.wincross.R

object CreateSTA {
  private const val TAG = "CreateSTA"
  private const val PREFS_NAME = "WinCross_preferences"
  private const val MOUNT_PREFERENCE_KEY = "mount_to_mnt"
  private const val IRREGULAR_MOUNT_PATH = "/mnt/Windows"

  // Map direktori yang diperlukan
  private fun getRequiredDirectories(windowsPath: String) = mapOf(
    "programDataSta" to "$windowsPath/ProgramData/sta",
    "sta" to "$windowsPath/sta",
    "desktop" to "$windowsPath/Users/Public/Desktop"
  )

  // Map file yang akan diekstrak
  private fun getFileExtractionMap(directories: Map<String, String>) = mapOf(
    "Switch to Android.lnk" to directories["desktop"]!!,
    "sta.exe" to directories["programDataSta"]!!,
    "sta.exe" to directories["sta"]!!,
    "sdd.exe" to directories["sta"]!!,
    "sdd.conf" to directories["sta"]!!,
    "boot_img_auto-flasher_V1.1.exe" to directories["sta"]!!
  )

  suspend fun RunSTAAsync(context: Context) = withContext(Dispatchers.IO) {
    try {
      val windowsPath = getWindowsPath(context)
      val directories = getRequiredDirectories(windowsPath)

      // Buat direktori yang diperlukan
      if (!createRequiredDirectories(directories.values.toList())) {
        Log.e(TAG, "Failed to create required directories")
        showError(context)
        return@withContext
      }

      // Ekstrak asset yang diperlukan
      if (!extractRequiredAssets(context, getFileExtractionMap(directories))) {
        Log.e(TAG, "Failed to extract required assets")
        showError(context)
        return@withContext
      }

      withContext(Dispatchers.Main) {
        DialogHelper.showPopupNotifications(context, "STA Created in Windows")
      }
      Log.d(TAG, "STA environment created successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Error creating STA environment: ${e.message}")
      showError(context)
    }
  }

  private fun getWindowsPath(context: Context): String {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    .let {
      prefs ->
      if (prefs.getBoolean(MOUNT_PREFERENCE_KEY, false)) {
        IRREGULAR_MOUNT_PATH
      } else {
        "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"
      }
    }
  }

  private fun createRequiredDirectories(directoryPaths: List<String>): Boolean {
    return directoryPaths.all {
      path ->
      File(path).let {
        dir ->
        dir.exists() || dir.mkdirs().also {
          created ->
          if (!created) Log.e(TAG, "Failed to create directory: $path")
        }
      }
    }
  }

  private fun extractRequiredAssets(
    context: Context,
    filesToExtract: Map<String, String>
  ): Boolean {
    return filesToExtract.all {
      (assetFile, destinationPath) ->
      try {
        AssetsManager.copyAssetFile(context, assetFile, destinationPath)
        Log.d(TAG, "Successfully extracted: $assetFile to $destinationPath")
        true
      } catch (e: Exception) {
        Log.e(TAG, "Failed to extract $assetFile: ${e.message}")
        false
      }
    }
  }

  private suspend fun showError(context: Context) {
    withContext(Dispatchers.Main) {
      DialogHelper.showPopupNotifications(
        context,
        context.getString(R.string.sta_creation_failed)
      )
    }
  }

  // Deprecated: Use RunSTAAsync instead
  @Deprecated("Use RunSTAAsync for better performance and error handling")
  fun RunSTA(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      RunSTAAsync(context)
    }
  }
}