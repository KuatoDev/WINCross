package id.vern.wincross.operations

import android.content.Context
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.managers.*
import id.vern.wincross.utils.*
import java.io.*
import java.net.*
import kotlinx.coroutines.*
import org.json.*
import id.vern.wincross.parsers.*

object QuickRotationDownloader {
  private const val TAG = "QuickRotationDownloader"
  private const val NOTIFICATION_ID = 3000

  private const val GITHUB_API_URL = "https://api.github.com/repos/Misha803/My-Scripts/releases"
  private const val RELEASE_TAG = "QuickRotate"
  private const val FILE_NAME = "QuickRotate_V2.1.exe"

  private const val FALLBACK_URL =
      "https://github.com/Misha803/My-Scripts/releases/download/QuickRotate/QuickRotate_V2.1.exe"

  fun downloadQuickRotation(context: Context) {
    Log.d(TAG, "Starting QuickRotationDownloader")
    val desktopPath = Utils.resolveDesktopPath(context)

    val directory = File(desktopPath)
    if (!directory.exists()) {
      val created = directory.mkdirs()
      Log.d(TAG, "Created directory $desktopPath: $created")
    }

    NotificationHelper.createNotificationChannel(context)
    val notificationBuilder =
        NotificationHelper.createDownloadNotification(context)
            .setContentTitle("Downloading Quick Rotation")

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
                destinationPath = desktopPath,
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
            Log.e(TAG, "Failed to download QuickRotation: ${e.message}", e)
            false
          }

      withContext(Dispatchers.Main) {
        NotificationHelper.showCompletionNotification(
            context = context, notificationId = NOTIFICATION_ID, success = success)

        if (success) {
          UtilityHelper.showToast(
              context, context.getString(R.string.download_successful, desktopPath))
          DialogHelper.showPopupNotifications(context, "Quick Rotation downloaded successfully")
          Log.d(TAG, "QuickRotation download completed successfully")
        } else {
          UtilityHelper.showToast(context, context.getString(R.string.download_failed))
          DialogHelper.showPopupNotifications(context, "Failed to download QuickRotation")
          Log.e(TAG, "Failed to download QuickRotation")
        }
      }
    }
  }
}