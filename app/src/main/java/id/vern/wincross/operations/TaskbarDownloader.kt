package id.vern.wincross.operations

import android.content.Context
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.managers.*
import id.vern.wincross.utils.*
import id.vern.wincross.parsers.*
import java.io.*
import java.net.*
import kotlinx.coroutines.*
import org.json.*

object TaskbarDownloader {
  private const val TAG = "TaskbarDownloader"
  private const val NOTIFICATION_ID = 3000

  private const val GITHUB_API_URL = "https://api.github.com/repos/Misha803/My-Scripts/releases"
  private const val RELEASE_TAG = "Optimized-Taskbar-Control"
  private const val FILE_NAME = "Optimized_Taskbar_Control_V3.0.exe"

  private const val FALLBACK_URL =
      "https://github.com/Misha803/My-Scripts/releases/download/Optimized-Taskbar-Control/Optimized_Taskbar_Control_V3.0.exe"

  fun downloadTaskbarControl(context: Context) {
    Log.d(TAG, "Starting downloadTaskbarControl")
    val deaktopPath = Utils.resolveDesktopPath(context)

    val directory = File(deaktopPath)
    if (!directory.exists()) {
      val created = directory.mkdirs()
      Log.d(TAG, "Created directory $deaktopPath: $created")
    }

    NotificationHelper.createNotificationChannel(context)
    val notificationBuilder =
        NotificationHelper.createDownloadNotification(context)
            .setContentTitle("Downloading Taskbar Control")

    CoroutineScope(Dispatchers.IO).launch {
      var downloadUrl = FALLBACK_URL
      var fileName = FILE_NAME

      try {
        val githubParser = GithubApiParser(
            githubApiUrl = GITHUB_API_URL,
            releaseTag = RELEASE_TAG,
            tag = TAG
        )
        
        val releaseInfo = githubParser.getLatestReleaseInfo()
        if (releaseInfo != null) {
          downloadUrl = releaseInfo.first
          fileName = releaseInfo.second
          Log.d(TAG, "Using download URL from GitHub API: $downloadUrl")
        } else {
          Log.d(TAG, "Using fallback URL: $downloadUrl")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch release info from GitHub API, using fallback URL", e)
      }

      val success =
          try {
            DownloadManager.downloadFile(
                context = context,
                url = downloadUrl,
                destinationPath = deaktopPath,
                fileName = fileName) { progress ->
                  withContext(Dispatchers.Main) {
                    NotificationHelper.updateDownloadProgress(
                        context = context,
                        notificationId = NOTIFICATION_ID,
                        builder = notificationBuilder,
                        text = "Downloading: $fileName",
                        progress = progress)
                  }
                }
          } catch (e: Exception) {
            Log.e(TAG, "Failed to download Taskbar Control: ${e.message}", e)
            false
          }

      withContext(Dispatchers.Main) {
        NotificationHelper.showCompletionNotification(
            context = context, notificationId = NOTIFICATION_ID, success = success)

        if (success) {
          UtilityHelper.showToast(
              context, context.getString(R.string.download_successful, deaktopPath))
          DialogHelper.showPopupNotifications(context, "Taskbar Control downloaded successfully")
          Log.d(TAG, "Taskbar Control download completed successfully")
        } else {
          UtilityHelper.showToast(context, context.getString(R.string.download_failed))
          DialogHelper.showPopupNotifications(context, "Failed to download Taskbar Control")
          Log.e(TAG, "Failed to download Taskbar Control")
        }
      }
    }
  }
}