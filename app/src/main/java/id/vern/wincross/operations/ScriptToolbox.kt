
package id.vern.wincross.operations

import android.content.Context
import android.util.Log
import id.vern.wincross.helpers.DialogHelper
import id.vern.wincross.managers.*
import id.vern.wincross.R
import java.io.File
import kotlinx.coroutines.*

object ScriptToolbox {
    private const val TAG = "ScriptToolbox"
    private const val PREFS_NAME = "WinCross_preferences"
    private const val PREF_MOUNT_TO_MNT = "Windows Mount Path"

    private data class ExtractPaths(
        val toolboxPath: String,
        val desktopPath: String
    )

    private data class FileGroup(
        val files: List<String>,
        val destinationPath: (ExtractPaths) -> String
    )

    private val fileGroups = mapOf(
        "toolbox" to FileGroup(
            files = listOf(
                "display.exe"
            ),
            destinationPath = {
                it.toolboxPath
            }
        ),
        "desktop" to FileGroup(
            files = listOf(
                "RotationShortcut.lnk",
                "RotationShortcutReverseLandscape.lnk"
            ),
            destinationPath = {
                it.desktopPath
            }
        )
    )

    fun extractScript(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val paths = getPaths(context)

                if (!validateDirectories(paths)) {
                    showExtractionResult(context, false)
                    return@launch
                }

                val success = extractFiles(context, paths)
                showExtractionResult(context, success)
            } catch (e: Exception) {
                Log.e(TAG, "Extraction failed: ${e.message}", e)
                showExtractionResult(context, false)
            }
        }
    }

    private fun getPaths(context: Context): ExtractPaths {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val basePath = prefs.getString(PREF_MOUNT_TO_MNT, null)

        return ExtractPaths(
            toolboxPath = "$basePath/Toolbox",
            desktopPath = "$basePath/Users/Public/Desktop"
        )
    }

    private fun validateDirectories(paths: ExtractPaths): Boolean {
        return try {
            File(paths.toolboxPath).apply { mkdirs() }.exists() && 
            File(paths.desktopPath).apply { mkdirs() }.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create directories: ${e.message}")
            false
        }
    }

    private suspend fun extractFiles(context: Context, paths: ExtractPaths): Boolean =
        withContext(Dispatchers.IO) {
            fileGroups.values.all { group ->
                group.files.all { file ->
                    try {
                        AssetsManager.copyAssetFile(
                            context = context,
                            assetFileName = file,
                            destinationDir = group.destinationPath(paths)
                        )
                        Log.d(TAG, "Successfully extracted: $file")
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to extract $file: ${e.message}")
                        false
                    }
                }
            }
        }

    private suspend fun showExtractionResult(context: Context, success: Boolean) {
        withContext(Dispatchers.Main) {
            val messageResId = if (success) {
                R.string.extract_script_success
            } else {
                R.string.extract_failed_general
            }

            DialogHelper.showPopupNotifications(
                context,
                context.getString(messageResId)
            )

            if (success) {
                Log.d(TAG, "Extraction completed successfully")
            } else {
                Log.w(TAG, "Extraction completed with errors")
            }
        }
    }
}