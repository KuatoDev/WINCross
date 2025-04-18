package id.vern.wincross.operations

import android.content.Context
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.managers.*
import java.io.*
import java.net.*
import kotlinx.coroutines.*
import id.vern.wincross.utils.*
import org.json.*
import id.vern.wincross.parsers.*

object HideModemDownloader {
  private const val TAG = "HideModemDownloader"
  private const val NOTIFICATION_ID = 3000

  private const val GITHUB_API_URL = "https://api.github.com/repos/Misha803/My-Scripts/releases"
  private const val RELEASE_TAG = "ModemHide"
  private const val FILE_NAME = "ModemHide_V2.0.exe"

  private const val FALLBACK_URL =
      "https://github.com/Misha803/My-Scripts/releases/download/ModemHide/ModemHide_V2.0.exe"

  fun downloadHideModem(context: Context) {
    Log.d(TAG, "Starting HideModemDownloader")
    val deaktopPath = Utils.resolveDesktopPath(context)

    val directory = File(deaktopPath)
    if (!directory.exists()) {
      val created = directory.mkdirs()
      Log.d(TAG, "Created directory $deaktopPath: $created")
    }

    NotificationHelper.createNotificationChannel(context)
    val notificationBuilder =
        NotificationHelper.createDownloadNotification(context)
            .setContentTitle("Downloading Hide Modem Partition")

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
            Log.e(TAG, "Failed to download QuickRotation: ${e.message}", e)
            false
          }

      withContext(Dispatchers.Main) {
        NotificationHelper.showCompletionNotification(
            context = context, notificationId = NOTIFICATION_ID, success = success)

        if (success) {
          DialogHelper.showPopupNotifications(context, context.getString(R.string.download_successful, deaktopPath))
          Log.d(TAG, "Hide Modem download completed successfully")
        } else {
          DialogHelper.showPopupNotifications(context, context.getString(R.string.download_failed))
          Log.e(TAG, "Failed to download Hide Modem")
        }
      }
    }
  }
}