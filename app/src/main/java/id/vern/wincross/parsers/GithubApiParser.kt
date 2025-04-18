package id.vern.wincross.parsers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GithubApiParser(
    private val githubApiUrl: String,
    private val releaseTag: String,
    private val tag: String = "GithubApiParser"
) {
    
    /**
     * Fetches the latest release information from GitHub API
     * @return Pair of download URL and asset name, or null if not found or on error
     */
    suspend fun getLatestReleaseInfo(): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(githubApiUrl)
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
                    val releases = JSONArray(response.toString())

                    for (i in 0 until releases.length()) {
                        val release = releases.getJSONObject(i)
                        if (release.getString("tag_name") == releaseTag) {
                            val assets = release.getJSONArray("assets")

                            for (j in 0 until assets.length()) {
                                val asset = assets.getJSONObject(j)
                                val assetName = asset.getString("name")

                                if (assetName.endsWith(".exe", ignoreCase = true)) {
                                    val downloadUrl = asset.getString("browser_download_url")
                                    return@withContext Pair(downloadUrl, assetName)
                                }
                            }
                        }
                    }
                } else {
                    Log.e(tag, "GitHub API request failed with response code: $responseCode")
                }

                null
            } catch (e: Exception) {
                Log.e(tag, "Error fetching release info from GitHub: ${e.message}", e)
                null
            }
        }
}