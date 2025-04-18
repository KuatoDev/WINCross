package id.vern.wincross.utils

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.File

object Utils {

  private var cachedPanelType: String? = null

  fun getPanelType(): String {
    cachedPanelType?.let {
      return it
    }

    val result = executeShellCommand("su -mm -c cat /proc/cmdline")
    val cmdline = result.out.joinToString(" ")

    val panelType =
        when {
          // Huaxing panels
          cmdline.contains("j20s_42") ||
              cmdline.contains("dsi_k82_42_02_0a_dual_cphy_video") ||
              cmdline.contains("j20s_42_02_0b") ||
              cmdline.contains("ft8756_huaxing") -> "Huaxing"

          // Tianma panels
          cmdline.contains("j20s_36") ||
              cmdline.contains("k82_36") ||
              cmdline.contains("dsi_k82_36_02_0b_dual_cphy_video") ||
              cmdline.contains("nt36675_tianma") ||
              cmdline.contains("tianma_fhd_nt36672a") -> "Tianma"

          // BOE panels
          cmdline.contains("boe_nt36525") ||
              cmdline.contains("boe_nt36520") ||
              cmdline.contains("boe_fhd_nt36525") -> "BOE"

          // LG panels
          cmdline.contains("lg_lp079qx1") ||
              cmdline.contains("lg_lp079qx2") ||
              cmdline.contains("lg_fhd_lp079qx1") -> "LG"

          // Samsung panels
          cmdline.contains("samsung_s6e3ha2") ||
              cmdline.contains("samsung_s6e3fa3") ||
              cmdline.contains("samsung_s6e8fa5") -> "Samsung"

          // Other panels
          cmdline.contains("ebbg_fhd_ft8719") -> "EBBG"
          cmdline.contains("fhd_ea8076_global") -> "Global"
          cmdline.contains("fhd_ea8076_f1mp_cmd") -> "F1MP"
          cmdline.contains("fhd_ea8076_f1p2_cmd") -> "F1P2"
          cmdline.contains("fhd_ea8076_f1p2_2") -> "F1P2_2"
          cmdline.contains("fhd_ea8076_f1_cmd") -> "F1"
          cmdline.contains("fhd_ea8076_cmd") -> "ea8076_cmd"
          else -> "Unknown Panel"
        }

    Log.d("Utils", "Panel type detected: $panelType")
    cachedPanelType = panelType
    return panelType
  }

  fun getDeviceModel(context: Context): String {
    try {
      val device = android.os.Build.DEVICE
      val model = device.trim().ifBlank { "Unknown Model" }.uppercase()

      Log.d("Utils", "Device model: $model")

      context
          .getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
          .edit()
          .putString("device_model", model)
          .apply()

      return model
    } catch (e: Exception) {
      Log.e("Utils", "Error getting device model: ${e.message}")
      return "UNKNOWN MODEL"
    }
  }

  fun getTotalRam(context: Context): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val totalRam = formatStorageSize(memoryInfo.totalMem)
    Log.d("Utils", "Total RAM: $totalRam")
    return totalRam
  }

  fun getActiveSlot(context: Context): String? {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    return when (prefs.getString("Active Slot", null)) {
      "slot_a" -> "Slot A"
      "slot_b" -> "Slot B"
      "a_only" -> "Non A/B"
      else -> null
    }
  }

  fun getTotalStorage(): String {
    return try {
      val statFs = StatFs(Environment.getExternalStorageDirectory().path)
      val totalBytes = statFs.totalBytes
      formatStorageSize(totalBytes)
    } catch (e: Exception) {
      Log.e("Utils", "Error getting storage info: ${e.message}")
      "Unknown"
    }
  }

  private fun formatStorageSize(bytes: Long): String {
    val gbSize = bytes.toDouble() / (1024 * 1024 * 1024)
    val standardSizes = listOf(1, 2, 4, 6, 8, 12, 16, 32, 64, 128, 256, 512, 1024)
    val roundedSize = standardSizes.firstOrNull { it >= gbSize } ?: standardSizes.last()

    Log.d("Utils", "Actual storage: ${String.format("%.2f", gbSize)} GB, Rounded: $roundedSize GB")
    return "$roundedSize GB"
  }

  val Shell.Result.isSuccess: Boolean
    get() = this.code == 0

  fun executeShellCommand(
      command: String,
      logTag: String = "ShellUtils",
      logSuccess: Boolean = false,
      logFailure: Boolean = true
  ): Shell.Result {
    val result = Shell.cmd(command).exec()
    val success = result.code == 0

    if (success && logSuccess) {
      Log.d(logTag, "Command executed successfully: $command")
    } else if (!success && logFailure) {
      Log.e(logTag, "Command failed: $command")
    }
    return result
  }

  fun getBatteryKernelProfile(): String? {
    val command = "cat /sys/class/power_supply/battery/charge_full | cut -c1-4"
    return try {
      val result = Shell.cmd(command).exec()
      if (result.isSuccess) {
        result.out.firstOrNull()?.trim()
      } else {
        Log.e("BatteryUtils", "Failed to get battery kernel profile")
        null
      }
    } catch (e: Exception) {
      Log.e("BatteryUtils", "Error getting battery kernel profile: ${e.message}")
      null
    }
  }

  fun getBatteryCapacity(context: Context): Double {
    return try {
      val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
      val powerProfile = powerProfileClass.getConstructor(Context::class.java).newInstance(context)
      powerProfileClass.getMethod("getBatteryCapacity").invoke(powerProfile) as Double
    } catch (e: Exception) {
      e.printStackTrace()
      0.0
    }
  }
  
  fun resolveDesktopPath(context: Context): String {
    val prefs = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    val windowsPath = prefs.getString("Windows Mount Path", null)

    if (windowsPath.isNullOrEmpty()) {
        throw IllegalStateException("Windows Mount Path not set in preference")
    }

    val usersPath = File("$windowsPath/Users")
    val user = usersPath.listFiles()
        ?.firstOrNull { 
            it.isDirectory &&
            it.name != "Public" &&
            it.name != "Default" &&
            it.name != "Default User" &&
            File(it, "Desktop").exists()
        }?.name ?: "Public"
    return "$windowsPath/Users/$user/Desktop"
    }
}
