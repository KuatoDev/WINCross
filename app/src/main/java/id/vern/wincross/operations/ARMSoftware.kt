package id.vern.wincross.operations

import android.content.Context
import android.os.Environment
import android.util.Log
import id.vern.wincross.helpers.DialogHelper
import id.vern.wincross.managers.*
import kotlinx.coroutines.*
import java.io.*
import id.vern.wincross.R

object ARMSoftware {
  private const val TAG = "ARMSoftware"
  private const val PREFS_NAME = "WinCross_preferences"
  private const val PREF_MOUNT_TO_MNT = "Windows Mount Path"

  private val urlFiles = listOf(
    "WorksOnWoa.url",
    "TestedSoftware.url",
    "ARMSoftware.url",
    "ARMRepo.url"
  )

  fun extractARMSoftware(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val windowsPath = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREF_MOUNT_TO_MNT, "${Environment.getExternalStorageDirectory().path}/WINCross/Windows")
        val destinationPath = "$windowsPath/Users/Public/Desktop/ARMSoftware"
        createDirectory(destinationPath)
        val result = extractFiles(context, destinationPath)
        showResult(context, result)
      } catch (e: Exception) {
        Log.e(TAG, "Extraction failed: ${e.message}", e)
        showError(context)
      }
    }
  }

  private fun createDirectory(path: String) {
    File(path).apply {
      if (!exists() && !mkdirs()) {
        throw IOException("Failed to create directory: $path")
      }
      Log.d(TAG, "Directory created/verified: $path")
    }
  }

  private suspend fun extractFiles(
    context: Context,
    destinationPath: String
  ): ExtractionResult = withContext(Dispatchers.IO) {
    var successCount = 0
    var failureCount = 0

    urlFiles.forEach {
      fileName ->
      try {
        AssetsManager.copyAssetFile(context, fileName, destinationPath)
        Log.d(TAG, "Successfully extracted: $fileName")
        successCount++
      } catch (e: Exception) {
        Log.e(TAG, "Failed to extract $fileName: ${e.message}")
        failureCount++
      }
    }

    when {
      failureCount == 0 -> ExtractionResult.SUCCESS
      successCount > 0 -> ExtractionResult.PARTIAL
      else -> ExtractionResult.FAILURE
    }
  }

  private suspend fun showResult(
    context: Context,
    result: ExtractionResult
  ) = withContext(Dispatchers.Main) {
    val (message, logMessage) = when (result) {
      ExtractionResult.SUCCESS -> {
        context.getString(R.string.arm_software_added) to
        context.getString(R.string.extract_completed)
      }
      ExtractionResult.PARTIAL -> {
        context.getString(R.string.extract_partially_completed) to
        context.getString(R.string.extract_partially_completed)
      }
      ExtractionResult.FAILURE -> {
        context.getString(R.string.extract_failed_general) to
        "Extraction failed completely"
      }
    }

    DialogHelper.showPopupNotifications(context, message)

    when (result) {
      ExtractionResult.SUCCESS -> Log.d(TAG, logMessage)
      ExtractionResult.PARTIAL -> Log.w(TAG, logMessage)
      ExtractionResult.FAILURE -> Log.e(TAG, logMessage)
    }
  }

  private suspend fun showError(context: Context) = withContext(Dispatchers.Main) {
    DialogHelper.showPopupNotifications(
      context,
      context.getString(R.string.extract_failed_general)
    )
  }

  private enum class ExtractionResult {
    SUCCESS, PARTIAL, FAILURE
  }
}