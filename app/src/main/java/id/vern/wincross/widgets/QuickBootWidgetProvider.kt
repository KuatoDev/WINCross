package id.vern.wincross.widgets

import android.app.*
import android.appwidget.*
import android.content.*
import android.os.*
import android.util.*
import android.widget.RemoteViews
import id.vern.wincross.R
import id.vern.wincross.operations.*
import kotlinx.coroutines.*
import id.vern.wincross.helpers.*

class QuickBootWidgetProvider : AppWidgetProvider() {
  private val TAG = "QuickBootWidgetProvider"
  private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
    for (appWidgetId in appWidgetIds) {
      updateWidget(context, appWidgetManager, appWidgetId)
    }
    scheduleWidgetUpdate(context)
  }

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    Log.d(TAG, "Widget enabled - scheduling updates")
    scheduleWidgetUpdate(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    Log.d(TAG, "Widget disabled - canceling updates")
    cancelWidgetUpdate(context)
    coroutineScope.cancel()
  }

  companion object {
    private const val ACTION_QUICK_BOOT = "id.vern.wincross.QUICK_BOOT"
    private const val ACTION_QUICK_MOUNT = "id.vern.wincross.QUICK_MOUNT"
    private const val ACTION_UPDATE_WIDGET = "id.vern.wincross.UPDATE_WIDGET"
    private const val UPDATE_INTERVAL = 30 * 60 * 1000L

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
      val views = RemoteViews(context.packageName, R.layout.widget_quickboot)

      // Set QuickBoot button click listener
      val bootIntent = Intent(context, QuickBootWidgetProvider::class.java).apply {
        action = ACTION_QUICK_BOOT
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      }
      val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }

      val bootPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        bootIntent,
        pendingFlags
      )
      views.setOnClickPendingIntent(R.id.btnQuickBoot, bootPendingIntent)

      // Set QuickMount button click listener
      val mountIntent = Intent(context, QuickBootWidgetProvider::class.java).apply {
        action = ACTION_QUICK_MOUNT
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      }

      val mountPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId + 1000, // Menggunakan offset agar berbeda dari pendingIntent sebelumnya
        mountIntent,
        pendingFlags
      )
      views.setOnClickPendingIntent(R.id.btnQuickMount, mountPendingIntent)

      // Update mount button state
      updateMountButtonState(context, views)

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateMountButtonState(context: Context, views: RemoteViews) {
      // Menggunakan SharedPreferences untuk mendapatkan status
      val prefs = context.getSharedPreferences("wincross_prefs", Context.MODE_PRIVATE)
      val isWindowsInstalled = prefs.getBoolean("windows_installed", false)
      val isWindowsMounted = prefs.getBoolean("windows_mounted", false)

      if (!isWindowsInstalled) {
        views.setTextViewText(R.id.tv_quick_mount, context.getString(R.string.windows_not_installed))
        // Menampilkan visual disabled button
        views.setInt(R.id.btnQuickMount, "setAlpha", 128) // 50% opacity
        return
      }

      if (isWindowsMounted) {
        views.setTextViewText(R.id.tv_quick_mount, context.getString(R.string.umount_windows))
        views.setInt(R.id.btnQuickMount, "setAlpha", 255) // 100% opacity
      } else {
        views.setTextViewText(R.id.tv_quick_mount, context.getString(R.string.mount_windows))
        views.setInt(R.id.btnQuickMount, "setAlpha", 255) // 100% opacity
      }
    }

    fun updateAllWidgets(context: Context) {
      val appWidgetManager = AppWidgetManager.getInstance(context)
      val appWidgetIds = appWidgetManager.getAppWidgetIds(
        ComponentName(context, QuickBootWidgetProvider::class.java)
      )

      if (appWidgetIds.isNotEmpty()) {
        Log.d("QuickBootWidget", "Updating all ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
          updateWidget(context, appWidgetManager, appWidgetId)
        }
      }
    }

    fun scheduleWidgetUpdate(context: Context) {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val intent = Intent(context, QuickBootWidgetProvider::class.java).apply {
        action = ACTION_UPDATE_WIDGET
      }

      val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }

      val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        pendingFlags
      )

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.ELAPSED_REALTIME,
          SystemClock.elapsedRealtime() + UPDATE_INTERVAL,
          pendingIntent
        )
      } else {
        alarmManager.setInexactRepeating(
          AlarmManager.ELAPSED_REALTIME,
          SystemClock.elapsedRealtime() + UPDATE_INTERVAL,
          UPDATE_INTERVAL,
          pendingIntent
        )
      }

      Log.d("QuickBootWidget", "Widget update scheduled every ${UPDATE_INTERVAL / (60 * 1000)} minutes")
    }

    fun cancelWidgetUpdate(context: Context) {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val intent = Intent(context, QuickBootWidgetProvider::class.java).apply {
        action = ACTION_UPDATE_WIDGET
      }

      val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }

      val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        pendingFlags
      )

      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()

      Log.d("QuickBootWidget", "Widget update schedule canceled")
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)

    when (intent.action) {
      ACTION_QUICK_BOOT -> {
        Log.d(TAG, "Quick boot action received")
        val dialogIntent = Intent(context, QuickBootDialogActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(dialogIntent)
      }
      ACTION_QUICK_MOUNT -> {
        Log.d(TAG, "Quick mount action received")
        val isWindowsInstalled =UtilityHelper.isWindowsInstalled(context)
        if (!isWindowsInstalled) {
          return
        }

        val isWindowsMounted = UtilityHelper.isWindowsMounted(context)
        val dialogIntent = Intent(context, QuickMountDialogActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
          putExtra("is_mounted", isWindowsMounted)
        }
        context.startActivity(dialogIntent)
      }
      ACTION_UPDATE_WIDGET -> {
        Log.d(TAG, "Update widget action received")
        updateAllWidgets(context)
        scheduleWidgetUpdate(context)
      }
      Intent.ACTION_BOOT_COMPLETED -> {
        Log.d(TAG, "Boot completed - reinstating widget updates")
        scheduleWidgetUpdate(context)
      }
    }
  }
}