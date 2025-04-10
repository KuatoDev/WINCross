package id.vern.wincross.utils

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell

object GetPartitions {

  private data class PartitionInfo(
    val partitionName: String,
    val key: String,
    val logMessage: String
  )

  private val partitionInfoList = listOf(
    PartitionInfo("win", "Windows", "Windows partition"),
    PartitionInfo("boot", "Boot", "Boot partition"),
    PartitionInfo("boot_a", "Boot A", "Boot A partition"),
    PartitionInfo("boot_b", "Boot B", "Boot B partition"),
    PartitionInfo("logo", "Logo", "Logo partition"),
    PartitionInfo("splash", "Splash", "Splash partition"),
    PartitionInfo("persist", "Persist", "Persist partition"),
    PartitionInfo("modemst1", "Modemst1", "Modemst1 partition"),
    PartitionInfo("modemst2", "Modemst2", "Modemst2 partition"),
    PartitionInfo("dtbo", "DTBO", "DTBO partition"),
    PartitionInfo("dtbo_a", "DTBO A", "DTBO A partition"),
    PartitionInfo("dtbo_b", "DTBO B", "DTBO B partition"),
    PartitionInfo("vbmeta", "VBMeta", "Vbmeta partition"),
    PartitionInfo("vbmeta_a", "VBMeta A", "Vbmeta A partition"),
    PartitionInfo("vbmeta_b", "VBMeta B", "Vbmeta B partition"),
    PartitionInfo("fsc", "FSC", "FSC partition"),
    PartitionInfo("fsg", "FSG", "FSG partition")
  )

  fun checkAndSaveActiveSlot(context: Context) {
    val result = Utils.executeShellCommand("getprop ro.boot.slot_suffix")
    val slotValue = when {
      result.isSuccess && result.out.isNotEmpty() && result.out[0].contains("a", ignoreCase = true) -> "slot_a"
      result.isSuccess && result.out.isNotEmpty() && result.out[0].contains("b", ignoreCase = true) -> "slot_b"
      else -> "a_only"
    }

    context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    .edit()
    .putString("Active Slot", slotValue)
    .apply()
    Log.d("GetPartitions", "Active slot saved as: $slotValue")
  }

  fun findAndSaveAllPartitions(context: Context) {
    partitionInfoList.forEach {
      info ->
      val path = findPartition(info.partitionName)
      if (!path.isNullOrBlank()) {
        savePartitionPath(context, info.key, path)
        Log.d("GetPartitions", "Saved ${info.logMessage}: $path")
      } else {
        Log.e("GetPartitions", "Partition not found: ${info.logMessage}")
      }
    }
  }

  private fun findPartition(partitionName: String): String? {
    val searchPaths = listOf(
      "/dev/block/bootdevice/by-name/",
      "/dev/block/by-name/"
    )

    for (path in searchPaths) {
      val command = "su -mm -c \"find $path -type l -o -type b | grep -E '/${partitionName}\$'\""
      val result = Utils.executeShellCommand(command)

      if (result.isSuccess && result.out.isNotEmpty()) {
        val filteredPaths = result.out.filter {
          it.endsWith("/$partitionName")
        }

        if (filteredPaths.isNotEmpty()) {
          return filteredPaths.first().trim()
        }
      }
    }
    return null
  }

  private fun savePartitionPath(context: Context, key: String, path: String) {
    context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    .edit()
    .putString(key, path)
    .apply()
  }

  fun checkAndSaveAllPartitionsIfNeeded(context: Context) {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val needsRefresh = partitionInfoList.any {
      info ->
      prefs.getString(info.key, null).isNullOrBlank()
    }

    if (needsRefresh) {
      findAndSaveAllPartitions(context)
    }
  }
}