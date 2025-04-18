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

object UsbHostmodeDownloader {
  private const val TAG = "UsbHostmodeDownloader"
  private const val NOTIFICATION_ID = 5000

  private const val GITHUB_API_URL = "https://api.github.com/repos/Misha803/My-Scripts/releases"
  private const val RELEASE_TAG = "USB-Host-Mode-Control"
  private const val FILE_NAME = "USB_Host_Mode_Control_V9.0.exe"

  private const val FALLBACK_URL =
      "https://github.com/Misha803/My-Scripts/releases/download/USB-Host-Mode-Control/USB_Host_Mode_Control_V9.0.exe"

  fun downloadUsbHostmode(context: Context) {
    Log.d(TAG, "Starting downloadUsbHostmode")
    val desktopPath = Utils.resolveDesktopPath(context)
    val directory = File(desktopPath)
    if (!directory.exists()) {
      val created = directory.mkdirs()
      Log.d(TAG, "Created directory $desktopPath: $created")
    }

    NotificationHelper.createNotificationChannel(context)
    val notificationBuilder =
        NotificationHelper.createDownloadNotification(context)
            .setContentTitle("Downloading USB Host Mode Control")

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
            Log.e(TAG, "Failed to download USB Host Mode Control: ${e.message}", e)
            false
          }

      withContext(Dispatchers.Main) {
        NotificationHelper.showCompletionNotification(
            context = context, notificationId = NOTIFICATION_ID, success = success)
        if (success) {
          DialogHelper.showPopupNotifications(
              context, context.getString(R.string.download_successful, desktopPath))
          Log.d(TAG, "USB Host Mode Control download completed successfully")
        } else {
          DialogHelper.showPopupNotifications(context, context.getString(R.string.download_failed))
          Log.e(TAG, "Failed to download USB Host Mode Control")
        }
      }
    }
  }
}