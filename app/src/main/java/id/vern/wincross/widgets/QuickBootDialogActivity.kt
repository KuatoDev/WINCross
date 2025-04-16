package id.vern.wincross.widgets

import android.app.Activity
import android.content.Context
import android.os.*
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import id.vern.wincross.R
import id.vern.wincross.operations.FlashOperation
import id.vern.wincross.helpers.UtilityHelper
import id.vern.wincross.utils.Utils
import kotlinx.coroutines.*

class QuickBootDialogActivity : Activity() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.switch_to_windows))
            .setMessage(getString(R.string.msg_flash_uefi))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                coroutineScope.launch {
                    FlashOperation.flashIt(this@QuickBootDialogActivity)
                    withContext(Dispatchers.Main) {
                        MaterialAlertDialogBuilder(this@QuickBootDialogActivity)
                            .setTitle("Reboot Required")
                            .setMessage("Flash operation completed. Reboot device now?")
                            .setPositiveButton("Reboot") { _, _ ->
                                try {
                                    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                                    powerManager.reboot(null)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to reboot using PowerManager: ${e.message}")
                                    Utils.executeShellCommand("su -mm -c reboot")
                                }
                            }
                            .setNegativeButton("Later") { _, _ ->
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "QuickBootDialogActivity"
    }
}