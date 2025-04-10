package id.vern.wincross.operations

import android.content.Context
import android.os.Environment
import android.util.Log
import id.vern.wincross.helpers.DialogHelper
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import id.vern.wincross.R

object ProvisionModem {
  private const val TAG = "ProvisionModem"
  private const val PREFS_NAME = "WinCross_preferences"
  private const val PREF_MOUNT_TO_MNT = "mount_to_mnt"

  fun provision(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val winPath = getWindowsPath(context)
        // Execute modem provisioning commands
        val result = executeProvisionCommands(winPath)

        // Display result
        showResult(context, result)
      } catch (e: Exception) {
        Log.e(TAG, "Modem provisioning failed: ${e.message}", e)
        showError(context, e.message ?: "Unknown error")
      }
    }
  }

  private fun getWindowsPath(context: Context): String {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    .let {
      prefs ->
      if (prefs.getBoolean(PREF_MOUNT_TO_MNT, false)) {
        "/mnt/Windows"
      } else {
        "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"
      }
    }
  }

  private suspend fun executeProvisionCommands(winPath: String): ProvisionResult = withContext(Dispatchers.IO) {
    try {
      // Command 1: Provision modemst1
      val result1 = executeShellCommand("su -mm -c dd if=/dev/block/by-name/modemst1 of=\$(find ${winPath}/Windows/System32/DriverStore/FileRepository -name qcremotefs8150.inf_arm64_*)/bootmodem_fs1")
      Log.d(TAG, "modemst1 provision result: $result1")

      // Command 2: Provision modemst2
      val result2 = executeShellCommand("su -mm -c dd if=/dev/block/by-name/modemst2 of=\$(find ${winPath}/Windows/System32/DriverStore/FileRepository -name qcremotefs8150.inf_arm64_*)/bootmodem_fs2")
      Log.d(TAG, "modemst2 provision result: $result2")

      // Check for errors in command output
      if (result1.contains("error", ignoreCase = true) ||
        result2.contains("error", ignoreCase = true)) {
        ProvisionResult.PARTIAL
      } else {
        ProvisionResult.SUCCESS
      }
    } catch (e: Exception) {
      Log.e(TAG, "Shell command execution failed: ${e.message}", e)
      ProvisionResult.FAILURE
    }
  }

  private fun executeShellCommand(command: String): String {
    val process = Runtime.getRuntime().exec(command)
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val error = BufferedReader(InputStreamReader(process.errorStream))
    val output = StringBuilder()

    var line: String?
    while (reader.readLine().also {
      line = it
    } != null) {
      output.append(line).append("\n")
    }

    while (error.readLine().also {
      line = it
    } != null) {
      output.append("ERROR: ").append(line).append("\n")
    }

    process.waitFor()
    return output.toString()
  }

  private suspend fun showResult(
    context: Context,
    result: ProvisionResult
  ) = withContext(Dispatchers.Main) {
    val (message, logMessage) = when (result) {
      ProvisionResult.SUCCESS -> {
        context.getString(R.string.modem_provision_success) to
        "Modem provisioning completed successfully"
      }
      ProvisionResult.PARTIAL -> {
        context.getString(R.string.modem_provision_partial) to
        "Modem provisioning partially completed"
      }
      ProvisionResult.FAILURE -> {
        context.getString(R.string.modem_provision_failed) to
        "Modem provisioning failed completely"
      }
    }

    DialogHelper.showPopupNotifications(context, message)

    when (result) {
      ProvisionResult.SUCCESS -> Log.d(TAG, logMessage)
      ProvisionResult.PARTIAL -> Log.w(TAG, logMessage)
      ProvisionResult.FAILURE -> Log.e(TAG, logMessage)
    }
  }

  private suspend fun showError(context: Context, errorMessage: String) = withContext(Dispatchers.Main) {
    val message = context.getString(R.string.modem_provision_failed)
    DialogHelper.showPopupNotifications(context, message)
    Log.e(TAG, "Error: $errorMessage")
  }

  private enum class ProvisionResult {
    SUCCESS, PARTIAL, FAILURE
  }
}