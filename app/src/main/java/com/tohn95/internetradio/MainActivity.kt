package com.tohn95.internetradio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tohn95.internetradio.playback.PlayerController
import com.tohn95.internetradio.ui.navigation.AppScaffold
import com.tohn95.internetradio.ui.theme.InternetRadioTheme
import com.tohn95.internetradio.ui.theme.LocalPalette
import com.tohn95.internetradio.ui.theme.ThemeRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var themeRepository: ThemeRepository
    @Inject lateinit var playerController: PlayerController

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            // Пока настройки не прочитаны (доли секунды при холодном старте) — пустой кадр вместо
            // чужой темы; при пересоздании экрана они уже в памяти и тема не мигает.
            val settings = themeRepository.current.collectAsStateWithLifecycle().value ?: return@setContent
            InternetRadioTheme(settings) {
                val palette = LocalPalette.current
                val view = LocalView.current
                LaunchedEffect(palette.isDark) {
                    val window = view.context.findActivity().window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !palette.isDark
                        isAppearanceLightNavigationBars = !palette.isDark
                    }
                }
                AppScaffold(playerController, wavesAnimated = settings.wavesAnimated)
            }
        }
    }
}

/** Разворачивает цепочку ContextWrapper до Activity — надёжнее прямого каста view.context. */
private tailrec fun android.content.Context.findActivity(): android.app.Activity = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> error("Context не содержит Activity")
}
