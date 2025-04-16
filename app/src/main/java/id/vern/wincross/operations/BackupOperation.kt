package id.vern.wincross.operations

import android.content.Context
import android.os.*
import android.util.Log
import id.vern.wincross.helpers.UtilityHelper
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import id.vern.wincross.R
import id.vern.wincross.utils.*

object BackupOperation {
  private val backupPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Backup"
  private const val PREFERENCES_NAME = "WinCross_preferences"

  private val backupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val mainHandler = Handler(Looper.getMainLooper())

  private fun getPartitionPath(context: Context, key: String): String? =
  context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  .getString("${key}", null)

  fun getBootPartitionPath(context: Context) = getPartitionPath(context, "Boot")
  fun getBootAPartitionPath(context: Context) = getPartitionPath(context, "Boot A")
  fun getBootBPartitionPath(context: Context) = getPartitionPath(context, "Boot B")
  fun getFSCPartitionPath(context: Context) = getPartitionPath(context, "FSC")
  fun getFSGPartitionPath(context: Context) = getPartitionPath(context, "FSG")
  fun getWindowsPartitionPath(context: Context) = getPartitionPath(context, "Windows")
  fun getLogoPartitionPath(context: Context) = getPartitionPath(context, "Logo")
  fun getSplashPartitionPath(context: Context) = getPartitionPath(context, "Splash")
  fun getDtboPartitionPath(context: Context) = getPartitionPath(context, "DTBO")
  fun getDtboAPartitionPath(context: Context) = getPartitionPath(context, "DTBO A")
  fun getDtboBPartitionPath(context: Context) = getPartitionPath(context, "DTBO B")
  fun getPersistPartitionPath(context: Context) = getPartitionPath(context, "Persist")
  fun getModem1stPartitionPath(context: Context) = getPartitionPath(context, "Modemst1")
  fun getModem2stPartitionPath(context: Context) = getPartitionPath(context, "Modemst2")
  fun getVbmetaPartitionPath(context: Context) = getPartitionPath(context, "VBMeta")
  fun getVbmetaAPartitionPath(context: Context) = getPartitionPath(context, "VBMeta A")
  fun getVbmetaBPartitionPath(context: Context) = getPartitionPath(context, "VBMeta B")

  interface BackupProgressListener {
    fun onBackupStarted(totalPartitions: Int)
    fun onPartitionBackupStarted(partitionName: String, index: Int, total: Int)
    fun onPartitionBackupCompleted(partitionName: String, success: Boolean)
    fun onAllBackupsCompleted()
  }

  private var backupProgressListener: BackupProgressListener? = null

  fun setBackupProgressListener(listener: BackupProgressListener?) {
    backupProgressListener = listener
  }

  private suspend fun backupPartition(context: Context, partitionPath: String, destinationPath: String): Boolean {
    return withContext(Dispatchers.IO) {
      Log.d("BackupOperation", "Starting backup: $partitionPath -> $destinationPath")

      // Periksa keberadaan partisi
      val partitionCheckResult = Utils.executeShellCommand(
        "su -mm -c test -e $partitionPath && echo exists || echo not found",
        "BackupOperation",
        logSuccess = true,
        logFailure = true
      )
      val partitionExists = partitionCheckResult.out.contains("exists")

      if (!partitionExists) {
        Log.e("BackupOperation", "ERROR: Partition not found: $partitionPath")
        mainHandler.post {
          UtilityHelper.showToast(context, "Partition not found: ${destinationPath.substringAfterLast("/")}")
        }
        return@withContext false
      }

      File(backupPath).apply {
        if (!exists()) mkdirs()
      }

      val command = "su -mm -c dd if=$partitionPath of=$destinationPath bs=4M"
      val result = Utils.executeShellCommand(command, "BackupOperation", logSuccess = true, logFailure = true)
      val success = result.isSuccess

      val message = if (success) {
        "Backup completed: ${destinationPath.substringAfterLast("/")}"
      } else {
        "Backup failed: ${destinationPath.substringAfterLast("/")}"
      }

      mainHandler.post {
        UtilityHelper.showToast(context, message)
      }
      Log.d("BackupOperation", message)

      success
    }
  }

  fun backupAll(context: Context) {
    backupScope.launch {
      val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      val activeSlot = prefs.getString("Active Slot", null)

      val partitions = when (activeSlot) {
        "a_only" -> listOfNotNull(
          getBootPartitionPath(context)?.let {
            it to "$backupPath/boot.img"
          },
          getFSCPartitionPath(context)?.let {
            it to "$backupPath/fsc.img"
          },
          getFSGPartitionPath(context)?.let {
            it to "$backupPath/fsg.img"
          },
          getLogoPartitionPath(context)?.let {
            it to "$backupPath/logo.img"
          },
          getSplashPartitionPath(context)?.let {
            it to "$backupPath/splash.img"
          },
          getDtboPartitionPath(context)?.let {
            it to "$backupPath/dtbo.img"
          },
          getPersistPartitionPath(context)?.let {
            it to "$backupPath/persist.img"
          },
          getModem1stPartitionPath(context)?.let {
            it to "$backupPath/modemst1.img"
          },
          getModem2stPartitionPath(context)?.let {
            it to "$backupPath/modemst2.img"
          },
          getVbmetaPartitionPath(context)?.let {
            it to "$backupPath/vbmeta.img"
          }
        )
        "slot_a" -> listOfNotNull(
          getBootAPartitionPath(context)?.let {
            it to "$backupPath/boot_a.img"
          },
          getDtboAPartitionPath(context)?.let {
            it to "$backupPath/dtbo_a.img"
          },
          getVbmetaAPartitionPath(context)?.let {
            it to "$backupPath/vbmeta_a.img"
          },
          getPersistPartitionPath(context)?.let {
            it to "$backupPath/persist.img"
          },
          getModem1stPartitionPath(context)?.let {
            it to "$backupPath/modemst1.img"
          },
          getModem2stPartitionPath(context)?.let {
            it to "$backupPath/modemst2.img"
          },
          getFSCPartitionPath(context)?.let {
            it to "$backupPath/fsc.img"
          },
          getFSGPartitionPath(context)?.let {
            it to "$backupPath/fsg.img"
          },
          getLogoPartitionPath(context)?.let {
            it to "$backupPath/logo.img"
          },
          getSplashPartitionPath(context)?.let {
            it to "$backupPath/splash.img"
          }
        )
        "slot_b" -> listOfNotNull(
          getBootBPartitionPath(context)?.let {
            it to "$backupPath/boot_b.img"
          },
          getDtboBPartitionPath(context)?.let {
            it to "$backupPath/dtbo_b.img"
          },
          getVbmetaBPartitionPath(context)?.let {
            it to "$backupPath/vbmeta_b.img"
          },
          getFSCPartitionPath(context)?.let {
            it to "$backupPath/fsc.img"
          },
          getFSGPartitionPath(context)?.let {
            it to "$backupPath/fsg.img"
          },
          getLogoPartitionPath(context)?.let {
            it to "$backupPath/logo.img"
          },
          getPersistPartitionPath(context)?.let {
            it to "$backupPath/persist.img"
          },
          getModem1stPartitionPath(context)?.let {
            it to "$backupPath/modemst1.img"
          },
          getModem2stPartitionPath(context)?.let {
            it to "$backupPath/modemst2.img"
          },
          getSplashPartitionPath(context)?.let {
            it to "$backupPath/splash.img"
          }
        )
        else -> {
          mainHandler.post {
            UtilityHelper.showToast(context, "Invalid active slot detected.")
          }
          Log.e("BackupOperation", "Invalid active slot")
          emptyList()
        }
      }

      if (partitions.isEmpty()) {
        mainHandler.post {
          UtilityHelper.showToast(context, "No partition paths set in preferences.")
        }
        Log.e("BackupOperation", "No partition paths available for backup.")
        return@launch
      }

      Log.d("BackupOperation", "Starting backup process for ${partitions.size} partitions.")

      mainHandler.post {
        UtilityHelper.showToast(context, "Starting backup of ${partitions.size} partitions...")
        backupProgressListener?.onBackupStarted(partitions.size)
      }

      val successCount = AtomicInteger(0)

      partitions.forEachIndexed {
        index, (partition, dest) ->
        val partitionName = dest.substringAfterLast("/")
        mainHandler.post {
          backupProgressListener?.onPartitionBackupStarted(partitionName, index + 1, partitions.size)
        }

        val success = backupPartition(context, partition, dest)

        if (success) {
          successCount.incrementAndGet()
        }

        mainHandler.post {
          backupProgressListener?.onPartitionBackupCompleted(partitionName, success)
        }
      }

      mainHandler.post {
        UtilityHelper.showToast(context, "Backup process completed: ${successCount.get()}/${partitions.size} successful")
        backupProgressListener?.onAllBackupsCompleted()
      }

      Log.d("BackupOperation", "Backup process completed: ${successCount.get()}/${partitions.size} successful")
    }
  }

  suspend fun backupBootToWindows(context: Context): Boolean {
    try {
      val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      val bootPath = when (prefs.getString("Active Slot", null)) {
        "a_only" -> getBootPartitionPath(context)
        "slot_a" -> getBootAPartitionPath(context)
        "slot_b" -> getBootBPartitionPath(context)
        else -> null
      }

      if (bootPath == null) {
        mainHandler.post {
          UtilityHelper.showToast(context, "Boot partition path not found for active slot")
        }
        return false
      }

      val kernelInWindows = if (prefs.getBoolean("Windows Mount Path", false)) "/mnt/Windows"
      else "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"

      if (!UtilityHelper.isWindowsMounted(context) && !MountWindows.mount(context)) {
        mainHandler.post {
          UtilityHelper.showToast(context, "Failed to mount Windows.")
        }
        return false
      }

      mainHandler.post {
        UtilityHelper.showToast(context, "Starting backup of boot partition to Windows...")
        backupProgressListener?.onBackupStarted(1)
        backupProgressListener?.onPartitionBackupStarted("boot.img", 1, 1)
      }

      val success = backupPartition(context, bootPath, "$kernelInWindows/boot.img")

      mainHandler.post {
        backupProgressListener?.onPartitionBackupCompleted("boot.img", success)
        backupProgressListener?.onAllBackupsCompleted()
      }

      Log.d("BackupOperation", "Backup completed for boot partition: $bootPath")
      return success
    } catch (e: Exception) {
      Log.e("BackupOperation", "Backup failed: ${e.message}")
      return false
    }
  }

  fun cancelAllBackupOperations() {
    backupScope.coroutineContext.cancelChildren()
    Log.d("BackupOperation", "All backup operations cancelled")
  }
}