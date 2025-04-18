package id.vern.wincross.operations

import android.app.*
import android.content.Context
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.managers.AssetsManager
import id.vern.wincross.managers.DownloadManager
import java.io.*
import id.vern.wincross.parsers.*
import id.vern.wincross.utils.*
import kotlin.math.roundToInt
import kotlinx.coroutines.*

object FrameworkDownloader {
  private const val TAG = "FrameworkDownloader"
  private const val SINGLE_NOTIFICATION_ID = 2000

  private object Lock

  private val frameworkFiles =
      listOf(
          "PhysX-9.13.0604-SystemSoftware-Legacy.msi",
          "PhysX_9.23.1019_SystemSoftware.exe",
          "xnafx40_redist.msi",
          "opengl.appx",
          "2005vcredist_x64.EXE",
          "2005vcredist_x86.EXE",
          "2008vcredist_x64.exe",
          "2008vcredist_x86.exe",
          "2010vcredist_x64.exe",
          "2010vcredist_x86.exe",
          "2012vcredist_x64.exe",
          "2012vcredist_x86.exe",
          "2013vcredist_x64.exe",
          "2013vcredist_x86.exe",
          "2015VC_redist.x64.exe",
          "2015VC_redist.x86.exe",
          "2022VC_redist.arm64.exe",
          "dxwebsetup.exe",
          "oalinst.exe")

  fun downloadFrameworks(context: Context) {
    Log.d(TAG, "Starting downloadFrameworks")

    val userpath = Utils.resolveDesktopPath(context)
    val frameworkPath = "$userpath/Frameworks"

    val directory = File(frameworkPath)
    if (!directory.exists()) {
      val created = directory.mkdirs()
      Log.d(TAG, "Created directory $frameworkPath: $created")
    }

    val installer = listOf("install.bat")
    installer.forEach { installerFile ->
      try {
        AssetsManager.copyAssetFile(context, installerFile, frameworkPath)
        Log.d(TAG, "Successfully copied $installerFile to $frameworkPath")
      } catch (e: IOException) {
        Log.e(TAG, "Failed to copy installer file: ${e.message}", e)
      }
    }

    NotificationHelper.createNotificationChannel(context)
    val notificationBuilder =
        NotificationHelper.createDownloadNotification(context)
            .setContentTitle("Downloading Framework Files")

    CoroutineScope(Dispatchers.IO).launch {
      val baseUrl = "https://github.com/n00b69/woasetup/releases/download/Installers/"
      val downloadJobs = mutableListOf<Deferred<Boolean>>()
      val results = mutableListOf<Boolean>()
      val totalFiles = frameworkFiles.size
      var completedFiles = 0

      frameworkFiles.forEach { fileName ->
        val job = async {
          val result =
              try {
                DownloadManager.downloadFile(
                    context = context,
                    url = baseUrl + fileName,
                    destinationPath = frameworkPath,
                    fileName = fileName) { progress ->
                      val currentCompletedFiles: Int
                      synchronized(Lock) { currentCompletedFiles = completedFiles }

                      withContext(Dispatchers.Main) {
                        NotificationHelper.updateDownloadProgress(
                            context = context,
                            notificationId = SINGLE_NOTIFICATION_ID,
                            builder = notificationBuilder,
                            text = "Downloading: $fileName",
                            progress =
                                ((currentCompletedFiles.toFloat() / totalFiles) * 100)
                                    .roundToInt() + (progress / totalFiles))
                      }
                    }
              } catch (e: Exception) {
                Log.e(TAG, "Failed to download $fileName: ${e.message}")
                false
              }

          if (result) {
            synchronized(Lock) { completedFiles++ }
          }

          result
        }
        downloadJobs.add(job)
      }

      downloadJobs.forEach { results.add(it.await()) }

      withContext(Dispatchers.Main) {
        val allSuccessful = results.all { it }

        NotificationHelper.showCompletionNotification(
            context = context, notificationId = SINGLE_NOTIFICATION_ID, success = allSuccessful)

        if (allSuccessful) {
          DialogHelper.showPopupNotifications(
              context, context.getString(R.string.download_successful, frameworkPath))
        } else {
          val failedCount = results.count { !it }
          DialogHelper.showPopupNotifications(
              context, "$failedCount framework components failed to download")
        }
      }
    }
  }
}
