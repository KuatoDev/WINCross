package id.vern.wincross.operations

import android.content.Context
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.DialogHelper
import id.vern.wincross.utils.Utils
import kotlinx.coroutines.*

object ProvisionModem {
  private const val TAG = "ProvisionModem"

  fun provision(context: Context) {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val winPath = prefs.getString("Windows Mount Path", null)

    if (winPath.isNullOrEmpty()) {
      Log.e(TAG, "Windows path not configured")
      CoroutineScope(Dispatchers.Main).launch {
        DialogHelper.showPopupNotifications(
            context, context.getString(R.string.modem_provision_failed))
      }
      return
    }

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val result = executeProvisionCommands(winPath)
        showResult(context, result)
      } catch (e: Exception) {
        Log.e(TAG, "Modem provisioning failed: ${e.message}", e)
        showError(context, e.message ?: "Unknown error")
      }
    }
  }

  private suspend fun executeProvisionCommands(winPath: String): ProvisionResult =
      withContext(Dispatchers.IO) {
        try {
          // Format the path correctly for shell command
          val escapedWinPath = winPath.replace(" ", "\\ ")

          // Find driver repository path directly with su command
          val findDriverCmd =
              "su -mm -c \"find ${escapedWinPath}/Windows/System32/DriverStore/FileRepository -name qcremotefs8150.inf_arm64_*\""
          val driverPathResult =
              Utils.executeShellCommand(findDriverCmd, logSuccess = true, logFailure = true)

          if (!driverPathResult.isSuccess) {
            Log.e(TAG, "Failed to find driver repository path")
            return@withContext ProvisionResult.FAILURE
          }

          // Command 1: Provision modemst1
          val modemst1Result =
              Utils.executeShellCommand(
                  "su -mm -c \"dd if=/dev/block/by-name/modemst1 of=${escapedWinPath}/Windows/System32/DriverStore/FileRepository/qcremotefs8150.inf_arm64*/bootmodem_fs1\"",
                  logSuccess = true,
                  logFailure = true)

          // Command 2: Provision modemst2
          val modemst2Result =
              Utils.executeShellCommand(
                  "su -mm -c \"dd if=/dev/block/by-name/modemst2 of=${escapedWinPath}/Windows/System32/DriverStore/FileRepository/qcremotefs8150.inf_arm64*/bootmodem_fs2\"",
                  logSuccess = true,
                  logFailure = true)

          // Set correct permissions for the files
          if (modemst1Result.isSuccess || modemst2Result.isSuccess) {
            Utils.executeShellCommand(
                "su -mm -c \"chmod 644 ${escapedWinPath}/Windows/System32/DriverStore/FileRepository/qcremotefs8150.inf_arm64*/bootmodem_fs1 ${escapedWinPath}/Windows/System32/DriverStore/FileRepository/qcremotefs8150.inf_arm64*/bootmodem_fs2\"",
                logSuccess = true,
                logFailure = true)
          }

          when {
            modemst1Result.isSuccess && modemst2Result.isSuccess -> ProvisionResult.SUCCESS
            !modemst1Result.isSuccess && !modemst2Result.isSuccess -> ProvisionResult.FAILURE
            else -> ProvisionResult.PARTIAL
          }
        } catch (e: Exception) {
          Log.e(TAG, "Shell command execution failed: ${e.message}", e)
          ProvisionResult.FAILURE
        }
      }

  private suspend fun showResult(context: Context, result: ProvisionResult) =
      withContext(Dispatchers.Main) {
        val messageResId =
            when (result) {
              ProvisionResult.SUCCESS -> R.string.modem_provision_success
              ProvisionResult.PARTIAL -> R.string.modem_provision_partial
              ProvisionResult.FAILURE -> R.string.modem_provision_failed
            }

        val logLevel =
            when (result) {
              ProvisionResult.SUCCESS -> Log.INFO
              ProvisionResult.PARTIAL -> Log.WARN
              ProvisionResult.FAILURE -> Log.ERROR
            }

        val message = context.getString(messageResId)
        DialogHelper.showPopupNotifications(context, message)

        when (logLevel) {
          Log.INFO -> Log.i(TAG, "Modem provisioning completed successfully")
          Log.WARN -> Log.w(TAG, "Modem provisioning partially completed")
          Log.ERROR -> Log.e(TAG, "Modem provisioning failed completely")
          else -> Log.i(TAG, "Unknown log level")
        }
      }

  private suspend fun showError(context: Context, errorMessage: String) =
      withContext(Dispatchers.Main) {
        DialogHelper.showPopupNotifications(
            context, context.getString(R.string.modem_provision_failed))
        Log.e(TAG, "Error: $errorMessage")
      }

  private enum class ProvisionResult {
    SUCCESS,
    PARTIAL,
    FAILURE
  }
}
