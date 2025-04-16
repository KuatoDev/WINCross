package id.vern.wincross.operations

import android.content.Context
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.managers.DownloadManager
import kotlinx.coroutines.*
import org.json.*
import java.io.*
import java.net.*

object TaskbarDownloader {
  private const val TAG = "TaskbarDownloader"
  private const val NOTIFICATION_ID = 3000
  
  // GitHub API constants
  private const val GITHUB_API_URL = "https://api.github.com/repos/Misha803/My-Scripts/releases"
  private const val RELEASE_TAG = "Optimized-Taskbar-Control"
  private const val FILE_NAME = "Optimized_Taskbar_Control_V3.0.exe"
  
  // Fallback direct URL in case API fails
  private const val FALLBACK_URL = "https://github.com/Misha803/My-Scripts/releases/download/Optimized-Taskbar-Control/Optimized_Taskbar_Control_V3.0.exe"

  /**
   * Downloads Taskbar Control from GitHub releases
   */
  fun downloadTaskbarControl(context: Context) {
    Log.d(TAG, "Starting downloadTaskbarControl")

    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val windowsPath = prefs.getString("Windows Mount Path", "")
    val taskbarPath = "$windowsPath/Users/Public/Desktop"

    val directory = File(taskbarPath)
    if (!directory.exists()) {
      val created = directory.mkdirs()
      Log.d(TAG, "Created directory $taskbarPath: $created")
    }

    NotificationHelper.createNotificationChannel(context)
    val notificationBuilder = NotificationHelper.createDownloadNotification(context)
      .setContentTitle("Downloading Taskbar Control")

    CoroutineScope(Dispatchers.IO).launch {
      // First try to get the download URL from GitHub API
      var downloadUrl = FALLBACK_URL
      var fileName = FILE_NAME
      
      try {
        val releaseInfo = getLatestReleaseInfo()
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

      // Now download the file
      val success = try {
        DownloadManager.downloadFile(
          context = context,
          url = downloadUrl,
          destinationPath = taskbarPath,
          fileName = fileName
        ) { progress ->
          withContext(Dispatchers.Main) {
            NotificationHelper.updateDownloadProgress(
              context = context,
              notificationId = NOTIFICATION_ID,
              builder = notificationBuilder,
              text = "Downloading: $fileName",
              progress = progress
            )
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to download Taskbar Control: ${e.message}", e)
        false
      }

      withContext(Dispatchers.Main) {
        NotificationHelper.showCompletionNotification(
          context = context,
          notificationId = NOTIFICATION_ID,
          success = success
        )

        if (success) {
          UtilityHelper.showToast(context, context.getString(R.string.download_successful, taskbarPath))
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

  /**
   * Fetches release information from GitHub API
   * @return Pair of download URL and file name, or null if API call fails
   */
  private suspend fun getLatestReleaseInfo(): Pair<String, String>? = withContext(Dispatchers.IO) {
    try {
      val url = URL(GITHUB_API_URL)
      val connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
      connection.connectTimeout = 15000
      connection.readTimeout = 15000
      
      val responseCode = connection.responseCode
      if (responseCode == HttpURLConnection.HTTP_OK) {
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
          response.append(line)
        }
        reader.close()
        
        // Parse the JSON response
        val releases = JSONArray(response.toString())
        
        // Find the release with our tag
        for (i in 0 until releases.length()) {
          val release = releases.getJSONObject(i)
          if (release.getString("tag_name") == RELEASE_TAG) {
            val assets = release.getJSONArray("assets")
            
            // Find the asset we want
            for (j in 0 until assets.length()) {
              val asset = assets.getJSONObject(j)
              val assetName = asset.getString("name")
              
              // Look for the EXE file
              if (assetName.endsWith(".exe", ignoreCase = true)) {
                val downloadUrl = asset.getString("browser_download_url")
                return@withContext Pair(downloadUrl, assetName)
              }
            }
          }
        }
      } else {
        Log.e(TAG, "GitHub API request failed with response code: $responseCode")
      }
      
      null
    } catch (e: Exception) {
      Log.e(TAG, "Error fetching release info from GitHub: ${e.message}", e)
      null
    }
  }

  /**
   * Checks if the Taskbar Control executable has already been downloaded
   * 
   * @param context Application context
   * @return True if the file exists, false otherwise
   */
  fun isTaskbarControlDownloaded(context: Context): Boolean {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val windowsPath = prefs.getString("Windows Mount Path", "")
    val taskbarPath = "$windowsPath/Users/Public/Desktop"
    val file = File(taskbarPath, FILE_NAME)
    return file.exists() && file.length() > 0
  }
  
  /**
   * Returns the path to the downloaded Taskbar Control executable file
   * 
   * @param context Application context
   * @return File path or null if the file doesn't exist
   */
  fun getTaskbarControlPath(context: Context): String? {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val windowsPath = prefs.getString("Windows Mount Path", "")
    val taskbarPath = "$windowsPath/Users/Public/Desktop"
    val file = File(taskbarPath, FILE_NAME)
    return if (file.exists() && file.length() > 0) {
      file.absolutePath
    } else {
      null
    }
  }
}