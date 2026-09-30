package com.tohn95.internetradio.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class RadioWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RadioWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Отладочное закрепление виджета с adb:
        // adb shell am broadcast -a com.tohn95.internetradio.PIN_WIDGET -n com.tohn95.internetradio/.widget.RadioWidgetReceiver
        if (intent.action == "com.tohn95.internetradio.PIN_WIDGET") {
            // requestPinAppWidget throws IllegalStateException if the app has no foreground
            // activity/service at call time (e.g. broadcast fired while backgrounded) — this is
            // debug-only tooling, so swallow rather than crash the process.
            try {
                val mgr = AppWidgetManager.getInstance(context)
                mgr.requestPinAppWidget(ComponentName(context, RadioWidgetReceiver::class.java), null, null)
            } catch (e: IllegalStateException) {
                android.util.Log.w("RadioWidgetReceiver", "requestPinAppWidget failed (no foreground activity?)", e)
            }
        }
    }
}
