package com.tohn95.internetradio

import android.app.Application
import com.tohn95.internetradio.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class InternetRadioApp : Application() {
    @Inject lateinit var widgetUpdater: WidgetUpdater

    override fun onCreate() {
        super.onCreate()
        widgetUpdater.start()
    }
}
