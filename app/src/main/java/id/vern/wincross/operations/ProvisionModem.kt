package id.vern.wincross.operations

import android.content.Context
import android.os.Environment
import android.util.Log
import id.vern.wincross.helpers.DialogHelper
import id.vern.wincross.helpers.UtilityHelper
import id.vern.wincross.utils.Utils
import kotlinx.coroutines.*
import id.vern.wincross.R

object ProvisionModem {
  private const val TAG = "ProvisionModem"
  private const val PREFS_NAME = "WinCross_preferences"
  private const val PREF_MOUNT_TO_MNT = "mount_to_mnt"

  fun provision(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val winPath = getWindowsPath(context)
        val result = executeProvisionCommands(winPath)
        showResult(context, result)
      } catch (e: Exception) {
        Log.e(TAG, "Modem provisioning failed: ${e.message}", e)
        showError(context, e.message ?: "Unknown error")
      }
    }
  }

  private fun getWindowsPath(context: Context): String =
  context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  .let {
    prefs ->
    if (prefs.getBoolean(PREF_MOUNT_TO_MNT, false)) "/mnt/Windows"
    else "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"
  }

  private suspend fun executeProvisionCommands(winPath: String): ProvisionResult = withContext(Dispatchers.IO) {
    try {
      // Command 1: Provision modemst1
      val modemst1Result = Utils.executeShellCommand(
        "su -mm -c dd if=/dev/block/by-name/modemst1 of=\\$(find ${winPath}/Windows/System32/DriverStore/FileRepository -name qcremotefs8150.inf_arm64_*)/bootmodem_fs1",
        logSuccess = true,
        logFailure = true
      )

      // Command 2: Provision modemst2
      val modemst2Result = Utils.executeShellCommand(
        "su -mm -c dd if=/dev/block/by-name/modemst2 of=\\$(find ${winPath}/Windows/System32/DriverStore/FileRepository -name qcremotefs8150.inf_arm64_*)/bootmodem_fs2",
        logSuccess = true,
        logFailure = true
      )

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

  private suspend fun showResult(
    context: Context,
    result: ProvisionResult
  ) = withContext(Dispatchers.Main) {
    val (messageResId, logLevel) = when (result) {
      ProvisionResult.SUCCESS -> R.string.modem_provision_success to Log.DEBUG
      ProvisionResult.PARTIAL -> R.string.modem_provision_partial to Log.WARN
      ProvisionResult.FAILURE -> R.string.modem_provision_failed to Log.ERROR
    }

    val message = context.getString(messageResId)
    DialogHelper.showPopupNotifications(context, message)

    when (logLevel) {
      Log.DEBUG -> Log.d(TAG, "Modem provisioning completed successfully")
      Log.WARN -> Log.w(TAG, "Modem provisioning partially completed")
      Log.ERROR -> Log.e(TAG, "Modem provisioning failed completely")
      else -> Log.i(TAG, "Unknown log level") // Added else branch
    }
  }

  private suspend fun showError(context: Context, errorMessage: String) = withContext(Dispatchers.Main) {
    DialogHelper.showPopupNotifications(
      context,
      context.getString(R.string.modem_provision_failed)
    )
    Log.e(TAG, "Error: $errorMessage")
  }

  private enum class ProvisionResult {
    SUCCESS, PARTIAL, FAILURE
  }
}