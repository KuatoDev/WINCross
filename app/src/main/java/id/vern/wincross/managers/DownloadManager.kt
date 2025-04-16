package id.vern.wincross.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

object DownloadManager {
  private const val BUFFER_SIZE = 8192

  suspend fun downloadFile(
    context: Context,
    url: String,
    destinationPath: String,
    fileName: String,
    progressCallback: suspend (Int) -> Unit
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
          Log.e("DownloadManager", "HTTP error: ${connection.responseCode}")
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
              progressCallback(progress)
            }
          }
        } finally {
          outputStream.close()
          inputStream.close()
        }

        val destinationFile = File(destinationPath, fileName)
        tempFile.copyTo(destinationFile, overwrite = true)
        tempFile.delete()

        Log.d("DownloadManager", "Download completed: $destinationFile")
        true
      } catch (e: Exception) {
        Log.e("DownloadManager", "Failed to download $fileName", e)
        false
      }
    }
  }
}