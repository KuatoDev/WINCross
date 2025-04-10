package id.vern.wincross.operations

import android.content.Context
import android.os.Environment
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.DialogHelper
import id.vern.wincross.utils.DownloadManager
import id.vern.wincross.helpers.NotificationHelper
import kotlinx.coroutines.*
import java.io.File

object ReviAtlas {
  private const val DOWNLOAD_NOTIFICATION_ID = 1001

  fun downloadReviAtlas(context: Context, fileName: String, osType: String) {
    Log.d("ReviAtlas", "Starting downloadReviAtlas for $fileName ($osType)")
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val windowsPath = if (prefs.getBoolean("mount_to_mnt", false)) "/mnt/Windows"
    else "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"

    val desktopPath = "$windowsPath/Users/Public/Desktop"
    val playbookUrl = "https://github.com/n00b69/modified-playbooks/releases/download/$osType/$fileName"
    val ameWizardUrl = "https://download.ameliorated.io/AME%20Wizard%20Beta.zip"

    val directory = File(desktopPath)
    if (!directory.exists()) {
      val created = directory.mkdirs()
      Log.d("ReviAtlas", "Created directory $desktopPath: $created")
    }

    NotificationHelper.createNotificationChannel(context)
    val notificationBuilder = NotificationHelper.createDownloadNotification(context)

    CoroutineScope(Dispatchers.IO).launch {
      var overallSuccess = true

      // Download playbook
      withContext(Dispatchers.IO) {
        val playbookSuccess = try {
          DownloadManager.downloadFile(
            context = context,
            url = playbookUrl,
            destinationPath = desktopPath,
            fileName = fileName,
            progressCallback = {
              progress ->
              withContext(Dispatchers.Main) {
                NotificationHelper.updateDownloadProgress(
                  context = context,
                  notificationId = DOWNLOAD_NOTIFICATION_ID,
                  builder = notificationBuilder,
                  text = "Downloading $fileName: $progress%",
                  progress = progress
                )
              }
            }
          )
        } catch (e: Exception) {
          Log.e("ReviAtlas", "Failed to download playbook: ${e.message}")
          false
        }
        if (!playbookSuccess) overallSuccess = false
      }

      // Download AME Wizard
      withContext(Dispatchers.IO) {
        val ameWizardSuccess = try {
          DownloadManager.downloadFile(
            context = context,
            url = ameWizardUrl,
            destinationPath = desktopPath,
            fileName = "AME Wizard Beta.zip",
            progressCallback = {
              progress ->
              withContext(Dispatchers.Main) {
                NotificationHelper.updateDownloadProgress(
                  context = context,
                  notificationId = DOWNLOAD_NOTIFICATION_ID,
                  builder = notificationBuilder,
                  text = "Downloading AME Wizard: $progress%",
                  progress = 50 + (progress / 2)
                )
              }
            }
          )
        } catch (e: Exception) {
          Log.e("ReviAtlas", "Failed to download AME Wizard: ${e.message}")
          false
        }
        if (!ameWizardSuccess) overallSuccess = false
      }

      withContext(Dispatchers.Main) {
        NotificationHelper.showCompletionNotification(
          context = context,
          notificationId = DOWNLOAD_NOTIFICATION_ID,
          success = overallSuccess
        )

        if (overallSuccess) {
          DialogHelper.showPopupNotifications(context, context.getString(R.string.reviatlas_success))
        } else {
          DialogHelper.showPopupNotifications(context, context.getString(R.string.download_error))
        }
      }
    }
  }
}