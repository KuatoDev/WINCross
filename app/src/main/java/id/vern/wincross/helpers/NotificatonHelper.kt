package id.vern.wincross.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

object NotificationHelper {
  private const val NOTIFICATION_CHANNEL_ID = "download_channel_silent"
  private var notificationChannelCreated = false

  fun createNotificationChannel(context: Context) {
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
        Log.d("NotificationHelper", "Silent notification channel created: $NOTIFICATION_CHANNEL_ID")
      }
      notificationChannelCreated = true
    }
  }

  fun createDownloadNotification(context: Context): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
    .setContentTitle("Downloading Files")
    .setContentText("Starting downloads...")
    .setSmallIcon(android.R.drawable.stat_sys_download)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .setOngoing(true)
    .setAutoCancel(false)
    .setProgress(100, 0, true)
  }

  fun updateDownloadProgress(
    context: Context,
    notificationId: Int,
    builder: NotificationCompat.Builder,
    text: String,
    progress: Int
  ) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val updatedNotification = builder
    .setContentText(text)
    .setProgress(100, progress, false)
    .build()
    notificationManager.notify(notificationId, updatedNotification)
  }

  fun showCompletionNotification(
    context: Context,
    notificationId: Int,
    success: Boolean
  ) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
    .setContentTitle(if (success) "Download Complete" else "Download Failed")
    .setContentText(if (success) "All files downloaded successfully" else "Failed to download some files")
    .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .setOngoing(false)
    .setAutoCancel(true)
    .build()

    notificationManager.notify(notificationId, notification)
  }
}