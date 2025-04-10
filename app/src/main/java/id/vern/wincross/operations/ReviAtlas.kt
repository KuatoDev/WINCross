package id.vern.wincross.operations

import android.content.Context
import android.os.*
import android.util.Log
import android.app.*
import androidx.core.app.NotificationCompat
import id.vern.wincross.helpers.*
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import kotlin.math.roundToInt
import id.vern.wincross.R

object ReviAtlas {
  private const val NOTIFICATION_CHANNEL_ID = "download_channel_silent"
  private const val DOWNLOAD_NOTIFICATION_ID = 1001
  private const val BUFFER_SIZE = 8192
  private var notificationChannelCreated = false

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

    createNotificationChannel(context)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
    .setContentTitle("Downloading ReviAtlas Files")
    .setContentText("Starting downloads...")
    .setSmallIcon(android.R.drawable.stat_sys_download)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .setOngoing(true)
    .setAutoCancel(false)
    .setProgress(100, 0, true)

    notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notificationBuilder.build())

    CoroutineScope(Dispatchers.IO).launch {
      var overallSuccess = true

      updateNotification(context, notificationBuilder, "Downloading $fileName", 0)
      val playbookSuccess = downloadFile(
        context,
        playbookUrl,
        desktopPath,
        fileName,
        notificationBuilder
      )
      if (!playbookSuccess) overallSuccess = false

      updateNotification(context, notificationBuilder, "Downloading AME Wizard", 50)
      val ameWizardSuccess = downloadFile(
        context,
        ameWizardUrl,
        desktopPath,
        "AME Wizard Beta.zip",
        notificationBuilder
      )
      if (!ameWizardSuccess) overallSuccess = false

      withContext(Dispatchers.Main) {
        if (overallSuccess) {
          val completedNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
          .setContentTitle("Download Complete")
          .setContentText("All files downloaded successfully")
          .setSmallIcon(android.R.drawable.stat_sys_download_done)
          .setPriority(NotificationCompat.PRIORITY_LOW)
          .setOngoing(false)
          .setAutoCancel(true)
          .build()

          notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, completedNotification)
          DialogHelper.showPopupNotifications(context, context.getString(R.string.reviatlas_success))
        } else {
          val errorNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
          .setContentTitle("Download Failed")
          .setContentText("Failed to download some files")
          .setSmallIcon(android.R.drawable.stat_notify_error)
          .setPriority(NotificationCompat.PRIORITY_LOW)
          .setOngoing(false)
          .setAutoCancel(true)
          .build()

          notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, errorNotification)
          DialogHelper.showPopupNotifications(context, context.getString(R.string.download_error))
        }
      }
    }
  }

  private fun createNotificationChannel(context: Context) {
    if (!notificationChannelCreated) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Silent Downloads"
        val descriptionText = "Shows download progress with minimal interruption"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
          description = descriptionText
          enableVibration(false)
          enableLights(false)
          setSound(null, null)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d("ReviAtlas", "Silent notification channel created: $NOTIFICATION_CHANNEL_ID")
      }
      notificationChannelCreated = true
    }
  }

  private suspend fun updateNotification(
    context: Context,
    builder: NotificationCompat.Builder,
    text: String,
    progress: Int
  ) {
    withContext(Dispatchers.Main) {
      try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val updatedNotification = builder
        .setContentText(text)
        .setProgress(100, progress, false)
        .build()
        notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, updatedNotification)
      } catch (e: Exception) {
        Log.e("ReviAtlas", "Error updating notification: ${e.message}", e)
      }
    }
  }

  private suspend fun downloadFile(
    context: Context,
    url: String,
    destinationPath: String,
    fileName: String,
    notificationBuilder: NotificationCompat.Builder
  ): Boolean {
    return withContext(Dispatchers.IO) {
      try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
          Log.e("downloadFile", "HTTP error: ${connection.responseCode}")
          return@withContext false
        }

        val fileLength = connection.contentLength
        val tempFile = File(context.cacheDir, fileName)
        var bytesRead: Int
        var totalRead = 0
        val buffer = ByteArray(BUFFER_SIZE)
        val inputStream = BufferedInputStream(connection.inputStream)
        val outputStream = FileOutputStream(tempFile)

        var lastProgressUpdate = 0

        try {
          while (inputStream.read(buffer).also {
            bytesRead = it
          } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalRead += bytesRead
            val progress = if (fileLength > 0) {
              (totalRead * 100 / fileLength.toFloat()).roundToInt()
            } else {
              -1
            }
            if (progress >= lastProgressUpdate + 5 || progress == 100) {
              lastProgressUpdate = progress
              withContext(Dispatchers.Main) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val updatedNotification = notificationBuilder
                .setContentText("Downloading $fileName: $progress%")
                .setProgress(100, progress, progress == -1)
                .build()
                notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, updatedNotification)
              }
            }
          }
        } finally {
          outputStream.close()
          inputStream.close()
        }

        val destinationFile = File(destinationPath, fileName)
        tempFile.copyTo(destinationFile, overwrite = true)
        tempFile.delete()

        Log.d("downloadFile", "Download completed: $destinationFile")
        true
      } catch (e: Exception) {
        Log.e("downloadFile", "Failed to download $fileName", e)
        false
      }
    }
  }
}