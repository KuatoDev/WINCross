package id.vern.wincross.managers

import android.content.Context
import android.util.Log
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.*
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*

object AssetsManager {
  private const val TAG = "AssetsManager"
  private const val BUFFER_SIZE = 8192
  private val FILES_TO_COPY =
      listOf(
          "mount.ntfs",
          "sdd.conf",
          "install.bat",
          "sta.exe",
          "sdd.exe",
          "RemoveEdge.bat",
          "Switch to Android.lnk",
          "ARMSoftware.url",
          "ARMRepo.url",
          "TestedSoftware.url",
          "WorksOnWoa.url")

  private val fileExistenceCache = ConcurrentHashMap<String, Boolean>()

  suspend fun copyAssetsToExecutableDir(context: Context): Int =
      withContext(Dispatchers.IO) {
        var successCount = 0
        fileExistenceCache.clear()
        Log.d(TAG, "Starting asset copy process for ${FILES_TO_COPY.size} files")

        FILES_TO_COPY.forEach { fileName ->
          val outputFile = File(context.filesDir, fileName)
          if (!UtilityHelper.isFileExists(outputFile.path)) {
            try {
              if (copySingleAsset(context, fileName, outputFile)) {
                successCount++
              }
            } catch (e: Exception) {
              Log.e(TAG, "Critical error copying $fileName: ${e.message}")
            }
          } else {
            Log.d(TAG, "$fileName already exists. Skipping copy.")
          }
        }

        Log.d(
            TAG, "Asset copy process completed. $successCount/${FILES_TO_COPY.size} files copied.")
        return@withContext successCount
      }

  private fun doesFileExist(file: File): Boolean {
    val path = file.absolutePath
    return fileExistenceCache.getOrPut(path) { file.exists() }
  }

  private fun copySingleAsset(context: Context, fileName: String, outputFile: File): Boolean {
    return try {
      Log.d(TAG, "Copying $fileName to app files directory...")
      val assetManager = context.assets
      assetManager.open(fileName).use { input ->
        FileOutputStream(outputFile).use { output -> input.copyTo(output) }
      }

      val permissionResult = setFullPermissions(outputFile.absolutePath)
      if (permissionResult) {
        fileExistenceCache[outputFile.absolutePath] = true
        Log.d(TAG, "Successfully copied and set permissions for $fileName")
        true
      } else {
        Log.e(TAG, "Failed to set permissions for $fileName")
        false
      }
    } catch (e: IOException) {
      Log.e(TAG, "Error copying $fileName: ${e.message}", e)
      false
    }
  }

  private fun setFullPermissions(filePath: String): Boolean {
    val shellResult = Utils.executeShellCommand("chmod 777 $filePath")

    if (!shellResult.isSuccess) {
      Log.e(TAG, "Shell chmod failed, attempting File API")
      val file = File(filePath)
      val fileSuccess =
          file.setExecutable(true, false) &&
              file.setReadable(true, false) &&
              file.setWritable(true, false)
      return fileSuccess
    }
    return true
  }

  @Throws(IOException::class)
  fun copyAssetFile(context: Context, assetFileName: String, destinationDir: String) {
    require(assetFileName.isNotBlank()) { "Asset file name cannot be blank" }

    val outFile =
        File(destinationDir, assetFileName).apply { parentFile?.takeIf { !it.exists() }?.mkdirs() }

    context.assets.open(assetFileName).use { input ->
      FileOutputStream(outFile).use { output -> input.copyTo(output, BUFFER_SIZE) }
    }
  }
}
