package com.tohn95.internetradio.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.tohn95.internetradio.playback.PlayerController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Зеркалит состояние плеера в GlanceState и перерисовывает виджет. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerController: PlayerController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            combine(playerController.currentStation, playerController.playRequested) { s, p -> s?.name to p }
                .distinctUntilChanged()
                .collect { (name, playing) ->
                    val mgr = GlanceAppWidgetManager(context)
                    val ids = mgr.getGlanceIds(RadioWidget::class.java)
                    ids.forEach { id ->
                        updateAppWidgetState(context, id) { prefs ->
                            if (name == null) prefs.remove(WidgetKeys.stationName)
                            else prefs[WidgetKeys.stationName] = name
                            prefs[WidgetKeys.playing] = playing
                        }
                    }
                    if (ids.isNotEmpty()) RadioWidget().updateAll(context)
                }
        }
    }
}
