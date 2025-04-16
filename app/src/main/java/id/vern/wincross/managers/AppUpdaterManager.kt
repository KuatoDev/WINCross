package id.vern.wincross.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AppUpdaterManager(private val context: Context) {

    private val TAG = "AppUpdater"
    private val GITHUB_API_URL = "https://api.github.com/repos/KuatoDev/WINCross/releases/latest"

    interface UpdateCallback {
        fun onUpdateAvailable(versionName: String, downloadUrl: String, releaseNotes: String)
        fun onNoUpdateAvailable()
        fun onError(error: String)
    }

    data class UpdateResult(
        val hasUpdate: Boolean = false,
        val newVersion: String = "",
        val downloadUrl: String = "",
        val releaseNotes: String = "",
        val error: String = ""
    )

    fun checkForUpdates(currentVersion: String, callback: UpdateCallback) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                fetchLatestRelease(currentVersion)
            }
            if (result.error.isNotEmpty()) {
                callback.onError(result.error)
            } else if (result.hasUpdate) {
                callback.onUpdateAvailable(result.newVersion, result.downloadUrl, result.releaseNotes)
            } else {
                callback.onNoUpdateAvailable()
            }
        }
    }

    private fun fetchLatestRelease(currentVersion: String): UpdateResult {
    return try {
        val url = URL(GITHUB_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        val responseCode = connection.responseCode
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()

            val jsonObject = JSONObject(response)
            val rawTag = jsonObject.getString("tag_name")
            
            val cleanedTag = rawTag
               .trim()
               .replace(Regex("^v", RegexOption.IGNORE_CASE), "")
               .trim()

            if (cleanedTag.isEmpty()) {
                return UpdateResult(error = "Invalid Version Tag: '$rawTag'")
            }

            var apkDownloadUrl = ""
            val assets = jsonObject.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.getString("name")
                if (assetName.endsWith(".apk")) {
                    apkDownloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            val compare = compareVersions(currentVersion, cleanedTag)
            val hasUpdate = compare < 0

            if (hasUpdate && apkDownloadUrl.isNotEmpty()) {
                UpdateResult(true, cleanedTag, apkDownloadUrl, jsonObject.getString("body"))
            } else {
                UpdateResult(false)
            }
        } else {
            Log.e(TAG, "Error response server: HTTP $responseCode")
            UpdateResult(error = "Error: HTTP $responseCode")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error checking for updates", e)
        UpdateResult(error = "Error: ${e.message}")
    }
}

    private fun compareVersions(version1: String, version2: String): Int {
    val v1Parts = version1.split(".")
    val v2Parts = version2.split(".")

    val maxLength = maxOf(v1Parts.size, v2Parts.size)

    for (i in 0 until maxLength) {
        val v1Part = if (i < v1Parts.size) v1Parts[i].toIntOrNull() ?: 0 else 0
        val v2Part = if (i < v2Parts.size) v2Parts[i].toIntOrNull() ?: 0 else 0
        if (v1Part < v2Part) return -1
        if (v1Part > v2Part) return 1
    }

    return 0
}

    fun downloadUpdate(downloadUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}