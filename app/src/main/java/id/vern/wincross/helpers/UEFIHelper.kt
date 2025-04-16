package id.vern.wincross.helpers

import android.content.*
import android.net.Uri
import android.os.Environment
import android.provider.*
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import id.vern.wincross.R
import java.util.Locale
import android.app.Activity
import org.json.JSONObject
import java.io.*
import java.net.*
import java.io.File
import kotlinx.coroutines.*
import org.json.JSONArray

object UEFIHelper {

  private const val TAG = "UEFIHelper"
  const val PREFS_NAME = "WinCross_preferences"
  const val SELECTED_UEFI_PATH = "UEFI"
  private const val GITHUB_API_BASE = "https://api.github.com/repos/"

  private var filePickerLauncher: ActivityResultLauncher<String>? = null
  private var currentContext: Context? = null

  private val sharedPreferences: SharedPreferences?
  get() = currentContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun initialize(context: Context, launcher: ActivityResultLauncher<String>? = null) {
    currentContext = context
    launcher?.let {
      filePickerLauncher = it
    }
  }

  fun selectUEFIFile() {
    if (filePickerLauncher == null) {
      Log.e(TAG, "File picker launcher not initialized")
      currentContext?.let {
        UtilityHelper.showToast(it, "Please select UEFI from main screen")
      }
      return
    }

    try {
      filePickerLauncher?.launch("*/*")
    } catch (ex: ActivityNotFoundException) {
      currentContext?.let {
        UtilityHelper.showToast(it, it.getString(R.string.error_no_file_manager))
      }
    }
  }

  fun clearSavedUEFI() {
    sharedPreferences?.edit()?.remove(SELECTED_UEFI_PATH)?.apply()
    currentContext?.let {
      ctx ->
      UtilityHelper.showToast(ctx, "Saved UEFI preferences cleared")
    }
  }

  fun getSavedUEFIPath(): String? {
    return sharedPreferences?.getString(SELECTED_UEFI_PATH, null)
  }

  fun handleSelectedFile(uri: Uri) {
    val filePath = getFilePathFromUri(uri)
    val fileName = getFileNameFromUri(uri)

    Log.d(TAG, "Selected file URI: $uri")
    Log.d(TAG, "Selected file path: $filePath")
    Log.d(TAG, "Selected file name: $fileName")

    val isImgFile = fileName?.lowercase(Locale.getDefault())?.endsWith(".img") == true

    currentContext?.let {
      ctx ->
      if (isImgFile) {
        sharedPreferences?.edit()?.putString(SELECTED_UEFI_PATH, filePath)?.apply()
        UtilityHelper.showToast(ctx, "UEFI file selected: $fileName")
      } else {
        UtilityHelper.showToast(ctx, ctx.getString(R.string.error_invalid_file))
        Log.w(TAG, "Invalid file type selected: $fileName")
      }
    }
  }

  private fun getFilePathFromUri(uri: Uri): String? {
    Log.d(TAG, "Getting file path for URI: $uri")
    val context = currentContext ?: return null

    return try {
      when {
        isDocumentUri(context, uri) -> {
          getPathForDocumentUri(uri)
        }
        uri.scheme == "content" -> {
          getDataColumn(uri, null, null)
        } else -> {
          uri.path
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting file path", e)
      null
    }
  }

  private fun isDocumentUri(context: Context, uri: Uri): Boolean {
    return try {
      val documentClass = Class.forName("android.provider.DocumentsContract")
      val isDocumentUriMethod = documentClass.getMethod("isDocumentUri", Context::class.java, Uri::class.java)
      isDocumentUriMethod.invoke(null, context, uri) as Boolean
    } catch (e: Exception) {
      Log.e(TAG, "Error checking isDocumentUri", e)
      false
    }
  }

  private fun getDocumentId(uri: Uri): String? {
    return try {
      val documentClass = Class.forName("android.provider.DocumentsContract")
      val getDocumentIdMethod = documentClass.getMethod("getDocumentId", Uri::class.java)
      getDocumentIdMethod.invoke(null, uri) as String
    } catch (e: Exception) {
      Log.e(TAG, "Error getting document ID", e)
      null
    }
  }

  private fun getPathForDocumentUri(uri: Uri): String? {
    val docId = getDocumentId(uri) ?: return null

    when {
      isExternalStorageDocument(uri) -> {
        val split = docId.split(":")
        if (split.size >= 2) {
          return "${Environment.getExternalStorageDirectory()}/${split[1]}"
        }
      }
      isDownloadsDocument(uri) -> {
        try {
          val contentUri = withAppendedId(
            Uri.parse("content://downloads/public_downloads"),
            docId.toLong()
          )
          return getDataColumn(contentUri, null, null)
        } catch (e: NumberFormatException) {
          Log.e(TAG, "Error parsing document ID as long", e)
        }
      }
      isMediaDocument(uri) -> {
        val split = docId.split(":")
        val contentUri = when (split[0]) {
          "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
          "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
          "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
          else -> null
        }
        return contentUri?.let {
          getDataColumn(it, "_id=?", arrayOf(split[1]))
        }
      }
    }
    return null
  }

  private fun withAppendedId(contentUri: Uri, id: Long): Uri {
    return Uri.withAppendedPath(contentUri, id.toString())
  }

  private fun getDataColumn(uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
    val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
    val context = currentContext ?: return null

    return try {
      context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use {
        cursor ->
        if (cursor.moveToFirst()) {
          val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
          cursor.getString(columnIndex)
        } else null
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting data column", e)
      null
    }
  }

  private fun isExternalStorageDocument(uri: Uri): Boolean =
  "com.android.externalstorage.documents" == uri.authority

  private fun isDownloadsDocument(uri: Uri): Boolean =
  "com.android.providers.downloads.documents" == uri.authority

  private fun isMediaDocument(uri: Uri): Boolean =
  "com.android.providers.media.documents" == uri.authority

  private fun getFileNameFromUri(uri: Uri): String? {
    val context = currentContext ?: return null

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use {
        cursor ->
        if (cursor.moveToFirst()) {
          val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (displayNameIndex != -1) {
            return cursor.getString(displayNameIndex)
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting file name from content resolver", e)
    }

    return uri.path?.substringAfterLast('/')
  }

  fun getCurrentVersion(): String {
    return try {
      val uefiPath = getSavedUEFIPath()
      if (uefiPath != null) {
        val fileName = File(uefiPath).name
        extractVersionFromFileName(fileName)
      } else {
        "0.0"
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting current version: ${e.message}")
      "0.0"
    }
  }

  suspend fun checkForUpdates(
    owner: String,
    repo: String
  ): Triple<Boolean, String, String> = withContext(Dispatchers.IO) {
    try {
      val currentVersion = getCurrentVersion()
      Log.d(TAG, "Checking updates for $owner/$repo")
      Log.d(TAG, "Current version before check: $currentVersion")

      val url = URL("${GITHUB_API_BASE}$owner/$repo/releases")
      val connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

      if (connection.responseCode == HttpURLConnection.HTTP_OK) {
        val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
          it.readText()
        }
        Log.d(TAG, "GitHub API Response: $response")

        val releases = JSONArray(response)
        var latestVersion = "0.0"
        var downloadUrl = ""

        for (i in 0 until releases.length()) {
          val release = releases.getJSONObject(i)
          val assets = release.getJSONArray("assets")

          for (j in 0 until assets.length()) {
            val asset = assets.getJSONObject(j)
            val fileName = asset.getString("name")
            Log.d(TAG, "Processing asset: $fileName")

            if (fileName.lowercase().endsWith(".img")) {
              val version = extractVersionFromFileName(fileName)
              Log.d(TAG, "Found UEFI image - Name: $fileName, Version: $version")

              if (compareVersions(latestVersion, version)) {
                latestVersion = version
                downloadUrl = asset.getString("browser_download_url")
                Log.d(TAG, "New latest version found: $latestVersion, URL: $downloadUrl")
              }
            } else {
              Log.d(TAG, "Skipping non-IMG file: $fileName")
            }
          }
        }

        val hasUpdate = compareVersions(currentVersion, latestVersion)
        Log.d(TAG, "Final check result - Current: $currentVersion, Latest: $latestVersion, Has Update: $hasUpdate, Download URL: $downloadUrl")
        Triple(hasUpdate, latestVersion, downloadUrl)
      } else {
        Log.e(TAG, "Failed to check updates: ${connection.responseCode}")
        Triple(false, getCurrentVersion(), "")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error checking updates", e)
      e.printStackTrace()
      Triple(false, getCurrentVersion(), "")
    }
  }

  private fun extractVersionFromFileName(fileName: String): String {
    return try {
      if (!fileName.lowercase().endsWith(".img")) {
        return "0.0"
      }

      Log.d(TAG, "Extracting version from filename: $fileName")
      val cleanFileName = fileName.replace("POCO.X3.PRO", "")
      .replace("POCO.X3.Pro", "")
      .replace("POCOX3Pro", "")

      Log.d(TAG, "Cleaned filename: $cleanFileName")
      val numberRegex = "(\\d+(?:\\.\\d+)?)".toRegex()
      val version = numberRegex.find(cleanFileName).let {
        matchResult ->
        matchResult?.value ?: "0.0"
      }

      Log.d(TAG, "Found version number: $version")
      if (!version.contains(".")) "$version.0" else version
    } catch (e: Exception) {
      Log.e(TAG, "Error extracting version: ${e.message}")
      "0.0"
    }
  }
  private fun compareVersions(currentVersion: String, newVersion: String): Boolean {
    try {
      val current = currentVersion.replace(".", "").toDoubleOrNull() ?: 0.0
      val new = newVersion.replace(".", "").toDoubleOrNull() ?: 0.0
      Log.d(TAG, "Comparing versions - Current: $currentVersion ($current), New: $newVersion ($new)")
      return new > current
    } catch (e: Exception) {
      Log.e(TAG, "Error comparing versions", e)
      return false
    }
  }
}