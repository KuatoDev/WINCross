package id.vern.wincross.operations

import android.app.Activity
import android.content.Context
import android.os.*
import android.util.Log
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.*
import java.io.File
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object FlashOperation {
    private val backupPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Backup"
    private val kernelInWindows = "${Environment.getExternalStorageDirectory().path}/WINCross/Windows/"
    private const val PREFERENCES_NAME = "WinCross_preferences"
    private const val BLOCK_SIZE = "8M"
    private const val TAG = "FlashOperation"

    fun flashIt(context: Context) {
        val coroutineScope = CoroutineScope(Dispatchers.Main)

        coroutineScope.launch {
            if (UtilityHelper.isDeviceRooted().not()) {
                UtilityHelper.showToast(
                    context,
                    context.getString(
                        R.string.flash_status,
                        context.getString(R.string.status_failed),
                        context.getString(R.string.reason_not_rooted)
                    )
                )
                Log.e(TAG, "Cannot flash - device not rooted")
                return@launch
            }

            if (!UtilityHelper.isWindowsMounted(context)) {
                Log.d(TAG, "Windows is not mounted, attempting to mount...")
                val mountSuccess = MountWindows.mount(context)
                if (!mountSuccess) {
                    UtilityHelper.showToast(context, "Failed to mount Windows")
                    Log.e(TAG, "Failed to mount Windows")
                    return@launch
                }
                Log.d(TAG, "Windows mounted successfully")
            } else {
                Log.d(TAG, "Windows is already mounted, proceeding...")
            }

            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            var selectedUefiPath = prefs.getString("UEFI", null)

            if (selectedUefiPath == null || !UtilityHelper.isFileExists(selectedUefiPath)) {
                Log.w(TAG, "UEFI file not found or not selected")

                val dialogResult = suspendCancellableCoroutine<Boolean> { continuation ->
                    if (context !is Activity) {
                        continuation.resume(false)
                        return@suspendCancellableCoroutine
                    }

                    val rootView = context.window?.decorView?.rootView
                        ?: run {
                            continuation.resume(false)
                            return@suspendCancellableCoroutine
                        }

                    DialogHelper.createDialog(
                        context = context,
                        rootView = rootView,
                        title = "UEFI Not Found",
                        message = "Please select a UEFI file to continue",
                        positiveButtonText = "Select UEFI",
                        negativeButtonText = "Cancel",
                        onPositive = {
                            UEFIHelper.initialize(context)
                            UEFIHelper.selectUEFIFile()
                            continuation.resume(true)
                        },
                        onNegative = {
                            continuation.resume(false)
                        }
                    )
                }

                if (!dialogResult) {
                    Log.i(TAG, "User canceled UEFI selection")
                    return@launch
                }

                delay(500)

                selectedUefiPath = prefs.getString("UEFI", null)
                if (selectedUefiPath == null || !UtilityHelper.isFileExists(selectedUefiPath)) {
                    UtilityHelper.showToast(
                        context,
                        context.getString(
                            R.string.flash_status,
                            context.getString(R.string.status_failed),
                            context.getString(R.string.reason_missing_files)
                        )
                    )
                    Log.e(TAG, "Selected UEFI file not found")
                    return@launch
                }
            }

            val backupIfEmpty = prefs.getBoolean("backup_boot_if_empty", false)
            val forceBackup = prefs.getBoolean("force_backup_to_win", false)
            val alwaysProvisionModem = prefs.getBoolean("always_provision_modem", false)

            if (backupIfEmpty || forceBackup) {
                val existingBootImgInWindows = File("$kernelInWindows/boot.img")
                val fileExists = existingBootImgInWindows.exists()
                if (forceBackup || !fileExists) {
                    val backupSuccess = withContext(Dispatchers.IO) { BackupOperation.backupBootToWindows(context) }
                    if (backupSuccess == false) {
                        Log.e(TAG, "Failed to backup boot partition")
                        UtilityHelper.showToast(context, "Failed to backup boot partition")
                    }
                }
            }

            if (alwaysProvisionModem) {
                val provisionSuccess = withContext(Dispatchers.IO) {
                    try {
                        ProvisionModem.provision(context)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during modem provisioning: ${e.message}")
                        false
                    }
                }

                if (!provisionSuccess) {
                    Log.e(TAG, "Failed to provision modem")
                }
            }

            val activeSlot = prefs.getString("Active Slot", null)
            val bootPath = when (activeSlot) {
                "a_only" -> BackupOperation.getBootPartitionPath(context)
                "slot_a" -> BackupOperation.getBootAPartitionPath(context)
                "slot_b" -> BackupOperation.getBootBPartitionPath(context)
                else -> null
            }

            if (bootPath == null) {
                UtilityHelper.showToast(
                    context,
                    context.getString(
                        R.string.flash_status,
                        context.getString(R.string.status_failed),
                        context.getString(R.string.reason_partition_not_found)
                    )
                )
                Log.e(TAG, "Boot partition path missing for slot: $activeSlot")
                return@launch
            }

            val flashTasks = mutableListOf(selectedUefiPath to bootPath)

            var allSuccessful = true
            for ((src, dest) in flashTasks) {
                val result = withContext(Dispatchers.IO) {
                    Utils.executeShellCommand("su -mm -c dd if=$src of=$dest bs=$BLOCK_SIZE conv=fsync")
                }

                if (!result.isSuccess) {
                    allSuccessful = false
                    Log.e(TAG, "Failed to flash from $src to $dest")

                    if (src == selectedUefiPath) {
                        UtilityHelper.showToast(
                            context,
                            context.getString(
                                R.string.flash_status,
                                context.getString(R.string.status_failed),
                                context.getString(R.string.reason_boot_failed)
                            )
                        )
                        return@launch
                    }
                }
            }

            val unmountSuccess = withContext(Dispatchers.IO) {
                try {
                    MountWindows.umount(context)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during unmount: ${e.message}")
                    false
                }
            }

            if (!unmountSuccess) {
                Log.e(TAG, "Failed to unmount Windows")
            }

            DialogHelper.showRebootDialog(context, allSuccessful)
        }
    }
}