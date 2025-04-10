package id.vern.wincross.operations

import android.content.Context
import android.os.Build
import android.util.Log
import android.app.*
import androidx.core.app.NotificationCompat
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.AssetsManager
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import kotlin.math.roundToInt
import id.vern.wincross.R

object FrameworkDownloader {
  private const val TAG = "FrameworkDownloader"
  private const val NOTIFICATION_CHANNEL_ID = "framework_download_channel"
  private const val SINGLE_NOTIFICATION_ID = 2000
  private const val BUFFER_SIZE = 8192

  private var notificationChannelCreated = false
  private object Lock

  private val frameworkFiles = listOf(
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
    "oalinst.exe"
  )

  fun downloadFrameworks(context: Context) {
    Log.d(TAG, "Starting downloadFrameworks")

    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val windowsPath = if (prefs.getBoolean("mount_to_mnt", false)) "/mnt/Windows"
    else "${context.getExternalFilesDir(null)?.path}/WINCross/Windows"

    val frameworkPath = "$windowsPath/Users/Public/Desktop/Frameworks"

    val directory = File(frameworkPath)
    if (!directory.exists()) {
      val created = directory.mkdirs()
      Log.d(TAG, "Created directory $frameworkPath: $created")
    }

    val installer = listOf("install.bat")
    installer.forEach {
      installerFile ->
      try {
        AssetsManager.copyAssetFile(context, installerFile, frameworkPath)
        Log.d(TAG, "Successfully copied $installerFile to $frameworkPath")
      } catch (e: IOException) {
        Log.e(TAG, "Failed to copy installer file: ${e.message}", e)
      }
    }

    createNotificationChannel(context)

    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
    .setContentTitle("Downloading Framework Files")
    .setContentText("Preparing downloads...")
    .setSmallIcon(android.R.drawable.stat_sys_download)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .setOngoing(true)
    .setAutoCancel(false)
    .setProgress(100, 0, true)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    .setSound(null)
    .setDefaults(0)

    try {
      notificationManager.notify(SINGLE_NOTIFICATION_ID, notificationBuilder.build())
    } catch (e: Exception) {
      Log.e(TAG, "Error posting initial notification: ${e.message}", e)
    }

    CoroutineScope(Dispatchers.IO).launch {
      val baseUrl = "https://github.com/n00b69/woasetup/releases/download/Installers/"
      val downloadJobs = mutableListOf<Deferred<Boolean>>()
      val results = mutableListOf<Boolean>()
      val totalFiles = frameworkFiles.size
      var completedFiles = 0
      val existingFiles = mutableListOf<String>()

      frameworkFiles.forEach {
        fileName ->
        val job = async {
          val result = downloadFileWithProgress(
            context,
            baseUrl + fileName,
            frameworkPath,
            fileName
          ) {
            isExisting, currentProgress ->
            if (isExisting) {
              existingFiles.add(fileName)
            }

            val currentCompletedFiles: Int
            val currentExistingSize: Int
            synchronized(Lock) {
              currentCompletedFiles = completedFiles
              currentExistingSize = existingFiles.size
            }

            val updatedProgress = ((currentCompletedFiles.toFloat() / totalFiles) * 100).roundToInt() +
            (currentProgress / totalFiles)

            withContext(Dispatchers.Main) {
              val currentFile = if (isExisting) "Skipped existing: $fileName" else "Downloading: $fileName"
              val updatedNotification = notificationBuilder
              .setContentTitle("Downloading Framework Files (${currentCompletedFiles + currentExistingSize}/$totalFiles)")
              .setContentText(currentFile)
              .setProgress(100, updatedProgress, false)
              .build()

              notificationManager.notify(SINGLE_NOTIFICATION_ID, updatedNotification)
            }
          }

          if (result) {
            synchronized(Lock) {
              completedFiles++
            }
          }

          result
        }
        downloadJobs.add(job)
      }

      downloadJobs.forEach {
        results.add(it.await())
      }

      withContext(Dispatchers.Main) {
        val successCount = results.count {
          it
        }
        val progressText = "$successCount/$totalFiles files completed"

        val finalNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setContentTitle("Framework Downloads Complete")
        .setContentText(progressText)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(false)
        .setAutoCancel(true)
        .setSound(null)
        .setDefaults(0)
        .build()

        notificationManager.notify(SINGLE_NOTIFICATION_ID, finalNotification)

        if (results.all {
          it
        }) {
          UtilityHelper.showToast(context, context.getString(R.string.download_successful, frameworkPath))
          DialogHelper.showPopupNotifications(context, "All framework components downloaded successfully")
        } else {
          val failedCount = results.count {
            !it
          }
          UtilityHelper.showToast(context, context.getString(R.string.download_failed))
          DialogHelper.showPopupNotifications(context, "$failedCount framework components failed to download")
        }
      }
    }
  }

  private fun createNotificationChannel(context: Context) {
    if (!notificationChannelCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Framework Download Progress"
      val descriptionText = "Shows download progress for framework files"
      val importance = NotificationManager.IMPORTANCE_MIN
      val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
        description = descriptionText
        enableVibration(false)
        enableLights(false)
      }
      val notificationManager = context.getSystemService(NotificationManager::class.java)
      notificationManager.createNotificationChannel(channel)

      Log.d(TAG, "Notification channel created: $NOTIFICATION_CHANNEL_ID")
      notificationChannelCreated = true
    }
  }

  private suspend fun downloadFileWithProgress(
    context: Context,
    url: String,
    destinationPath: String,
    fileName: String,
    progressCallback: suspend (isExisting: Boolean, currentProgress: Int) -> Unit
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.connectTimeout = 15000
      connection.readTimeout = 15000
      connection.setRequestProperty("User-Agent", "Mozilla/5.0")
      connection.connect()

      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        Log.e(TAG, "HTTP error: ${connection.responseCode} for $fileName")
        return@withContext false
      }

      val fileLength = connection.contentLength

      val destinationFile = File(destinationPath, fileName)
      if (destinationFile.exists() && fileLength > 0 && destinationFile.length() == fileLength.toLong()) {
        Log.d(TAG, "File $fileName already exists with correct size, skipping download")
        progressCallback(true, 100)
        return@withContext true
      }

      progressCallback(false, 0)

      val tempFile = File(context.cacheDir, fileName)
      BufferedInputStream(connection.inputStream).use {
        inputStream ->
        FileOutputStream(tempFile).use {
          outputStream ->
          var totalRead = 0
          var lastProgressUpdate = 0
          val buffer = ByteArray(BUFFER_SIZE)
          var bytesRead: Int

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
              progressCallback(false, progress)
            }
          }
        }
      }

      tempFile.copyTo(destinationFile, overwrite = true)
      tempFile.delete()

      progressCallback(false, 100)
      Log.d(TAG, "Download completed: $destinationFile")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to download $fileName", e)
      false
    }
  }
}