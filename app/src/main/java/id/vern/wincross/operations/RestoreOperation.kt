package id.vern.wincross.operations;

import android.content.Context
import android.os.*
import android.util.Log
import id.vern.wincross.helpers.UtilityHelper
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import id.vern.wincross.utils.*

object RestoreOperation {
  private val backupPath = "${Environment.getExternalStorageDirectory().path}/WINCross/Backup"
  private const val PREFERENCES_NAME = "WinCross_preferences"

  private val restoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
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

  interface RestoreProgressListener {
    fun onRestoreStarted(totalPartitions: Int)
    fun onPartitionRestoreStarted(partitionName: String, index: Int, total: Int)
    fun onPartitionRestoreCompleted(partitionName: String, success: Boolean)
    fun onAllRestoresCompleted()
  }

  private var restoreProgressListener: RestoreProgressListener? = null

  fun setRestoreProgressListener(listener: RestoreProgressListener?) {
    restoreProgressListener = listener
  }

  private suspend fun restorePartition(context: Context, sourcePath: String, partitionPath: String): Boolean {
    return withContext(Dispatchers.IO) {
      Log.d("RestoreOperation", "Starting restore: $sourcePath -> $partitionPath")

      val backupFile = File(sourcePath)
      if (!backupFile.exists()) {
        Log.e("RestoreOperation", "ERROR: Backup file not found: $sourcePath")
        mainHandler.post {
          UtilityHelper.showToast(context, "Backup file not found: ${sourcePath.substringAfterLast("/")}")
        }
        return@withContext false
      }

      val partitionCheckResult = Utils.executeShellCommand(
        "su -mm -c test -e $partitionPath && echo exists || echo not found",
        "RestoreOperation",
        logSuccess = true,
        logFailure = true
      )
      val partitionExists = partitionCheckResult.out.contains("exists")

      if (!partitionExists) {
        Log.e("RestoreOperation", "ERROR: Partition not found: $partitionPath")
        mainHandler.post {
          UtilityHelper.showToast(context, "Partition not found: ${partitionPath}")
        }
        return@withContext false
      }

      val command = "su -mm -c dd if=$sourcePath of=$partitionPath bs=4M"
      val result = Utils.executeShellCommand(command, "RestoreOperation", logSuccess = true, logFailure = true)
      val success = result.isSuccess

      val message = if (success) {
        "Restore completed: ${sourcePath.substringAfterLast("/")}"
      } else {
        "Restore failed: ${sourcePath.substringAfterLast("/")}"
      }

      mainHandler.post {
        UtilityHelper.showToast(context, message)
      }
      Log.d("RestoreOperation", message)

      success
    }
  }

  fun restoreAll(context: Context, selectedPartitions: List<String>? = null) {
    restoreScope.launch {
      val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      val activeSlot = prefs.getString("Active Slot", null)

      val allPartitions = when (activeSlot) {
        "a_only" -> listOfNotNull(
          getBootPartitionPath(context)?.let {
            Pair("boot", "$backupPath/boot.img" to it)
          },
          getFSCPartitionPath(context)?.let {
            Pair("fsc", "$backupPath/fsc.img" to it)
          },
          getFSGPartitionPath(context)?.let {
            Pair("fsg", "$backupPath/fsg.img" to it)
          },
          getLogoPartitionPath(context)?.let {
            Pair("logo", "$backupPath/logo.img" to it)
          },
          getSplashPartitionPath(context)?.let {
            Pair("splash", "$backupPath/splash.img" to it)
          },
          getDtboPartitionPath(context)?.let {
            Pair("dtbo", "$backupPath/dtbo.img" to it)
          },
          // Persist partition removed from list to avoid restoring it
          getModem1stPartitionPath(context)?.let {
            Pair("modemst1", "$backupPath/modemst1.img" to it)
          },
          getModem2stPartitionPath(context)?.let {
            Pair("modemst2", "$backupPath/modemst2.img" to it)
          },
          getVbmetaPartitionPath(context)?.let {
            Pair("vbmeta", "$backupPath/vbmeta.img" to it)
          }
        )
        "slot_a" -> listOfNotNull(
          getBootAPartitionPath(context)?.let {
            Pair("boot_a", "$backupPath/boot_a.img" to it)
          },
          getDtboAPartitionPath(context)?.let {
            Pair("dtbo_a", "$backupPath/dtbo_a.img" to it)
          },
          getVbmetaAPartitionPath(context)?.let {
            Pair("vbmeta_a", "$backupPath/vbmeta_a.img" to it)
          },
          // Persist partition removed from list to avoid restoring it
          getModem1stPartitionPath(context)?.let {
            Pair("modemst1", "$backupPath/modemst1.img" to it)
          },
          getModem2stPartitionPath(context)?.let {
            Pair("modemst2", "$backupPath/modemst2.img" to it)
          },
          getFSCPartitionPath(context)?.let {
            Pair("fsc", "$backupPath/fsc.img" to it)
          },
          getFSGPartitionPath(context)?.let {
            Pair("fsg", "$backupPath/fsg.img" to it)
          },
          getLogoPartitionPath(context)?.let {
            Pair("logo", "$backupPath/logo.img" to it)
          },
          getSplashPartitionPath(context)?.let {
            Pair("splash", "$backupPath/splash.img" to it)
          }
        )
        "slot_b" -> listOfNotNull(
          getBootBPartitionPath(context)?.let {
            Pair("boot_b", "$backupPath/boot_b.img" to it)
          },
          getDtboBPartitionPath(context)?.let {
            Pair("dtbo_b", "$backupPath/dtbo_b.img" to it)
          },
          getVbmetaBPartitionPath(context)?.let {
            Pair("vbmeta_b", "$backupPath/vbmeta_b.img" to it)
          },
          getFSCPartitionPath(context)?.let {
            Pair("fsc", "$backupPath/fsc.img" to it)
          },
          getFSGPartitionPath(context)?.let {
            Pair("fsg", "$backupPath/fsg.img" to it)
          },
          getLogoPartitionPath(context)?.let {
            Pair("logo", "$backupPath/logo.img" to it)
          },
          // Persist partition removed from list to avoid restoring it
          getModem1stPartitionPath(context)?.let {
            Pair("modemst1", "$backupPath/modemst1.img" to it)
          },
          getModem2stPartitionPath(context)?.let {
            Pair("modemst2", "$backupPath/modemst2.img" to it)
          },
          getSplashPartitionPath(context)?.let {
            Pair("splash", "$backupPath/splash.img" to it)
          }
        )
        else -> {
          mainHandler.post {
            UtilityHelper.showToast(context, "Invalid active slot detected.")
          }
          Log.e("RestoreOperation", "Invalid active slot")
          emptyList()
        }
      }


      val partitions = if (selectedPartitions != null && selectedPartitions.isNotEmpty()) {
        allPartitions.filter {
          (key, _) ->
          selectedPartitions.contains(key) && key != "persist"
        }.map {
          (_, pathPair) -> pathPair
        }
      } else {
        allPartitions.map {
          (_, pathPair) -> pathPair
        }
      }

      if (partitions.isEmpty()) {
        mainHandler.post {
          UtilityHelper.showToast(context, "No partition paths set in preferences.")
        }
        Log.e("RestoreOperation", "No partition paths available for restore.")
        return@launch
      }

      val validPartitions = partitions.filter {
        (source, _) ->
        val exists = File(source).exists()
        val isPersist = source.endsWith("persist.img")

        if (!exists) {
          Log.d("RestoreOperation", "Backup file not found: $source - skipping")
        } else if (isPersist) {
          Log.d("RestoreOperation", "Found persist.img but skipping it as it may be dangerous to restore")
          mainHandler.post {
            UtilityHelper.showToast(context, "Skipping persist.img for safety reasons")
          }
        }

        exists && !isPersist
      }

      if (validPartitions.isEmpty()) {
        mainHandler.post {
          UtilityHelper.showToast(context, "No backup files found to restore.")
        }
        Log.e("RestoreOperation", "No backup files found to restore.")
        return@launch
      }

      Log.d("RestoreOperation", "Starting restore process for ${validPartitions.size} partitions.")

      mainHandler.post {
        UtilityHelper.showToast(context, "Starting restore of ${validPartitions.size} partitions...")
        restoreProgressListener?.onRestoreStarted(validPartitions.size)
      }

      val successCount = AtomicInteger(0)

      validPartitions.forEachIndexed {
        index, (source, partition) ->
        val partitionName = source.substringAfterLast("/")
        mainHandler.post {
          restoreProgressListener?.onPartitionRestoreStarted(partitionName, index + 1, validPartitions.size)
        }

        val success = restorePartition(context, source, partition)

        if (success) {
          successCount.incrementAndGet()
        }

        mainHandler.post {
          restoreProgressListener?.onPartitionRestoreCompleted(partitionName, success)
        }
      }

      mainHandler.post {
        UtilityHelper.showToast(context, "Restore process completed: ${successCount.get()}/${validPartitions.size} successful")
        restoreProgressListener?.onAllRestoresCompleted()
      }

      Log.d("RestoreOperation", "Restore process completed: ${successCount.get()}/${validPartitions.size} successful")
    }
  }

  fun restoreBootFromWindows(context: Context) {
    restoreScope.launch {
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
        return@launch
      }

      val kernelInWindows = if (prefs.getBoolean("mount_to_mnt", false)) "/mnt/Windows"
      else "${Environment.getExternalStorageDirectory().path}/WINCross/Windows"

      val bootBackupFile = "$kernelInWindows/boot.img"

      if (!UtilityHelper.isWindowsMounted(context) && !MountWindows.mount(context)) {
        mainHandler.post {
          UtilityHelper.showToast(context, "Failed to mount Windows.")
        }
        return@launch
      }

      if (!File(bootBackupFile).exists()) {
        mainHandler.post {
          UtilityHelper.showToast(context, "Boot backup file not found in Windows partition")
        }
        return@launch
      }

      mainHandler.post {
        UtilityHelper.showToast(context, "Starting restore of boot partition from Windows...")
        restoreProgressListener?.onRestoreStarted(1)
        restoreProgressListener?.onPartitionRestoreStarted("boot.img", 1, 1)
      }

      val success = restorePartition(context, bootBackupFile, bootPath)

      mainHandler.post {
        restoreProgressListener?.onPartitionRestoreCompleted("boot.img", success)
        restoreProgressListener?.onAllRestoresCompleted()
      }

      Log.d("RestoreOperation", "Restore completed for boot partition from Windows")
    }
  }

  fun restoreSinglePartition(context: Context, partitionKey: String) {
    restoreScope.launch {
      if (partitionKey == "persist") {
        mainHandler.post {
          UtilityHelper.showToast(context, "Restore of persist partition is not allowed as it may be dangerous")
        }
        Log.d("RestoreOperation", "Restore of persist partition was attempted but prevented")
        return@launch
      }

      val partitionPath = getPartitionPath(context, partitionKey)
      if (partitionPath == null) {
        mainHandler.post {
          UtilityHelper.showToast(context, "Partition path not found for: $partitionKey")
        }
        return@launch
      }

      val backupFile = "$backupPath/${partitionKey}.img"
      if (!File(backupFile).exists()) {
        mainHandler.post {
          UtilityHelper.showToast(context, "Backup file not found for: $partitionKey")
        }
        return@launch
      }

      mainHandler.post {
        UtilityHelper.showToast(context, "Starting restore of $partitionKey partition...")
        restoreProgressListener?.onRestoreStarted(1)
        restoreProgressListener?.onPartitionRestoreStarted("$partitionKey.img", 1, 1)
      }

      val success = restorePartition(context, backupFile, partitionPath)

      mainHandler.post {
        restoreProgressListener?.onPartitionRestoreCompleted("$partitionKey.img", success)
        restoreProgressListener?.onAllRestoresCompleted()
      }

      Log.d("RestoreOperation", "Restore completed for $partitionKey partition")
    }
  }

  fun cancelAllRestoreOperations() {
    restoreScope.coroutineContext.cancelChildren()
    Log.d("RestoreOperation", "All restore operations cancelled")
  }
}