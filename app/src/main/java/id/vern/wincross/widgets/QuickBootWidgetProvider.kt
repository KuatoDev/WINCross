package id.vern.wincross.widgets

import android.app.*
import android.appwidget.*
import android.content.*
import android.os.*
import android.util.*
import android.widget.RemoteViews
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.operations.*
import kotlinx.coroutines.*

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
    private const val ACTION_UPDATE_WIDGET = "id.vern.wincross.UPDATE_WIDGET"
    private const val UPDATE_INTERVAL = 30 * 60 * 1000L

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
      val views = RemoteViews(context.packageName, R.layout.widget_quickboot)

      // Set QuickBoot button click listener
      val bootIntent =
          Intent(context, QuickBootWidgetProvider::class.java).apply {
            action = ACTION_QUICK_BOOT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
          }
      val pendingFlags =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          } else {
            PendingIntent.FLAG_UPDATE_CURRENT
          }

      val bootPendingIntent =
          PendingIntent.getBroadcast(context, appWidgetId, bootIntent, pendingFlags)
      views.setOnClickPendingIntent(R.id.btnQuickBoot, bootPendingIntent)

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun updateAllWidgets(context: Context) {
      val appWidgetManager = AppWidgetManager.getInstance(context)
      val appWidgetIds =
          appWidgetManager.getAppWidgetIds(
              ComponentName(context, QuickBootWidgetProvider::class.java))

      if (appWidgetIds.isNotEmpty()) {
        Log.d("QuickBootWidget", "Updating all ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
          updateWidget(context, appWidgetManager, appWidgetId)
        }
      }
    }

    fun scheduleWidgetUpdate(context: Context) {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val intent =
          Intent(context, QuickBootWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_WIDGET
          }

      val pendingFlags =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          } else {
            PendingIntent.FLAG_UPDATE_CURRENT
          }

      val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, pendingFlags)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + UPDATE_INTERVAL,
            pendingIntent)
      } else {
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + UPDATE_INTERVAL,
            UPDATE_INTERVAL,
            pendingIntent)
      }

      Log.d(
          "QuickBootWidget",
          "Widget update scheduled every ${UPDATE_INTERVAL / (60 * 1000)} minutes")
    }

    fun cancelWidgetUpdate(context: Context) {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val intent =
          Intent(context, QuickBootWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_WIDGET
          }

      val pendingFlags =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          } else {
            PendingIntent.FLAG_UPDATE_CURRENT
          }

      val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, pendingFlags)

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
        val dialogIntent =
            Intent(context, QuickBootDialogActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
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
