package com.tohn95.internetradio.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tohn95.internetradio.MainActivity
import com.tohn95.internetradio.R
import com.tohn95.internetradio.playback.PlayerController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetKeys {
    val stationName = stringPreferencesKey("station_name")
    val playing = booleanPreferencesKey("playing")
}

class RadioWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val pc = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).playerController()
        val liveName = pc.currentStation.value?.name
        val livePlaying = pc.playRequested.value
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val name = liveName ?: prefs[WidgetKeys.stationName]
            val playing = if (liveName != null) livePlaying else (prefs[WidgetKeys.playing] ?: false)
            GlanceTheme {
                Row(
                    modifier = GlanceModifier.fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp)
                        .clickable(actionRunCallback<OpenAppAction>()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name ?: context.getString(R.string.widget_idle),
                        style = TextStyle(color = GlanceTheme.colors.onSurface),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Image(
                        provider = ImageProvider(
                            if (playing) android.R.drawable.ic_media_pause
                            else android.R.drawable.ic_media_play
                        ),
                        contentDescription = null,
                        modifier = GlanceModifier.size(32.dp)
                            .clickable(actionRunCallback<TogglePlayAction>()),
                    )
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun playerController(): PlayerController
}

class TogglePlayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val pc = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).playerController()
        if (pc.currentStation.value != null) {
            withContext(Dispatchers.Main) { pc.togglePlayPause() }
        } else {
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
