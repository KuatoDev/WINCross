package id.vern.wincross.managers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.app.AlarmManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import id.vern.wincross.activity.MainActivity
import android.os.Environment  

class PermissionManager(private val activity: MainActivity) {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val EXACT_ALARM_REQUESTED_KEY = "exact_alarm_requested"
    }

    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("Permission", if (isGranted) "Permission granted" else "Permission denied")
    }

    fun checkAndRequestAll() {
        checkNotificationPermission()
        checkManageAllFilesPermission()
        checkScheduleExactAlarmPermission()
        checkRequiredPermissions()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun checkManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                Log.e("Permission", "Failed to open permission request: ${e.message}")
            }
        }
    }

    private fun checkScheduleExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms() &&
                !activity.sharedPreferences.getBoolean(EXACT_ALARM_REQUESTED_KEY, false)
            ) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                    activity.sharedPreferences.edit()
                        .putBoolean(EXACT_ALARM_REQUESTED_KEY, true)
                        .apply()
                } catch (e: Exception) {
                    Log.e("Permission", "Failed to open permission request: ${e.message}")
                }
            }
        }
    }

    private fun checkRequiredPermissions() {
        val permissions = listOf(
            Manifest.permission.RECEIVE_BOOT_COMPLETED
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.withIndex()
                .filter { grantResults[it.index] != PackageManager.PERMISSION_GRANTED }
            
            if (deniedPermissions.isNotEmpty()) {
                deniedPermissions.forEach {
                    Log.d("Permission", "Permission denied: ${it.value}")
                }
            } else {
                Log.d("Permission", "All permissions granted")
            }
        }
    }
}