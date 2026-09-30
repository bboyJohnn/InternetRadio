package com.tohn95.internetradio.ui.screens.settings

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import java.util.Locale
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tohn95.internetradio.R
import com.tohn95.internetradio.ui.theme.LocalPalette
import com.tohn95.internetradio.ui.theme.Oklch
import com.tohn95.internetradio.ui.theme.ThemeMode
import kotlin.math.roundToInt

private enum class SettingsTab(val label: Int) {
    GENERAL(R.string.settings_tab_general), VISUAL(R.string.settings_tab_visual), ABOUT(R.string.settings_tab_about),
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val palette = LocalPalette.current
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.settings_title), color = palette.text, fontSize = 26.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        )
        // Переключатель вкладок — «пилюля» с подсвеченной активной половиной.
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(50))
                .background(palette.cardBg).padding(4.dp),
        ) {
            SettingsTab.entries.forEachIndexed { i, t ->
                val selected = tab == i
                val bg by animateColorAsState(if (selected) palette.primary else Color.Transparent, label = "tabBg")
                val fg by animateColorAsState(
                    if (selected) (if (palette.isDark) palette.pageBg else Color.White) else palette.textMuted, label = "tabFg",
                )
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(50)).background(bg)
                        .clickable { tab = i }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(t.label), color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        AnimatedContent(
            targetState = tab, transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize(), label = "settingsTab",
        ) { t ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (t) {
                    SettingsTab.GENERAL.ordinal -> GeneralTab()
                    SettingsTab.VISUAL.ordinal -> VisualTab(viewModel)
                    else -> AboutTab()
                }
            }
        }
    }
}

/** Карточка-раздел настроек. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.cardBg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = palette.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun VisualTab(viewModel: SettingsViewModel) {
    val palette = LocalPalette.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var hueLocal by remember(settings.hue) { mutableIntStateOf(settings.hue) }
    var saturationLocal by remember(settings.saturation) { mutableIntStateOf(settings.saturation) }

    Section(stringResource(R.string.settings_color_section)) {
        // «Цвета из обоев» — только Android 12+ (Material You). Включены — ползунки и пипетка не нужны.
        val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        if (dynamicSupported) {
            SwitchRow(
                stringResource(R.string.settings_dynamic), stringResource(R.string.settings_dynamic_hint),
                checked = settings.dynamicColor, onChange = { viewModel.setDynamicColor(it) },
            )
        }
        if (!(dynamicSupported && settings.dynamicColor)) {
        Text(stringResource(R.string.settings_hue, hueLocal), color = palette.text, fontSize = 14.sp)
        // радужный жёлоб как в DIV (_rainbow_stops: 13 стопов oklch(.8,.1,i*30))
        Column {
            Spacer(
                Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient((0..12).map { Oklch.toColor(0.8, 0.1, it * 30.0) }))
            )
            Slider(
                value = hueLocal.toFloat(), valueRange = 0f..360f,
                onValueChange = { hueLocal = it.roundToInt() },
                onValueChangeFinished = { viewModel.setHue(hueLocal) },
            )
        }
        Text(stringResource(R.string.settings_saturation, saturationLocal), color = palette.text, fontSize = 14.sp)
        Slider(
            value = saturationLocal.toFloat(), valueRange = 50f..160f,
            onValueChange = { saturationLocal = it.roundToInt() },
            onValueChangeFinished = { viewModel.setSaturation(saturationLocal) },
        )
        Text(stringResource(R.string.settings_presets), color = palette.text, fontSize = 14.sp)
        // «пипетка» DIV: образец -> srgb_to_oklch -> hue+sat (main_window.py:691-693)
        val swatches = listOf(
            0xFFdd8736, 0xFFe05252, 0xFFd76bb1, 0xFF9a6be0, 0xFF5c7cfa, 0xFF3b82f6,
            0xFF2bb8c9, 0xFF2fbf71, 0xFF88c057, 0xFFc9b458, 0xFF8d6e63, 0xFF7f8c99,
        ).map { Color(it) }
        LazyVerticalGrid(
            GridCells.Fixed(6), Modifier.height(96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
        ) {
            items(swatches) { color ->
                // Box: ячейка сетки навязывает свою ширину — без обёртки круг тянулся в овал.
                Box(contentAlignment = Alignment.Center) { Spacer(
                    Modifier.size(40.dp).clip(CircleShape).background(color)
                        .clickable {
                            val (_, c, h) = Oklch.fromSrgb(
                                (color.red * 255).roundToInt(), (color.green * 255).roundToInt(), (color.blue * 255).roundToInt(),
                            )
                            val newHue = h.roundToInt()
                            val newSat = ((c / 0.14) * 100).roundToInt().coerceIn(50, 160)
                            hueLocal = newHue
                            saturationLocal = newSat
                            viewModel.setHue(newHue)
                            viewModel.setSaturation(newSat)
                        }
                ) }
            }
        }
        }
    }

    Section(stringResource(R.string.settings_look_section)) {
        Text(stringResource(R.string.settings_theme), color = palette.text, fontSize = 14.sp)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark),
            ).forEachIndexed { i, (mode, label) ->
                SegmentedButton(
                    selected = settings.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(i, 3),
                ) { Text(label) }
            }
        }
        SwitchRow(
            stringResource(R.string.settings_true_black), stringResource(R.string.settings_true_black_hint),
            checked = settings.trueBlack, onChange = { viewModel.setTrueBlack(it) },
        )
        SwitchRow(
            stringResource(R.string.settings_waves), stringResource(R.string.settings_waves_hint),
            checked = settings.wavesAnimated, onChange = { viewModel.setWaves(it) },
        )
    }
}

/** Строка-переключатель настроек: заголовок, пояснение мелким, Switch справа. */
@Composable
private fun SwitchRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    // Переключается нажатием на всю строку, а не только на сам Switch.
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = palette.text, fontSize = 14.sp)
            Text(hint, color = palette.textMuted, fontSize = 12.sp, lineHeight = 15.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AboutTab() {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }

    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Адаптивная иконка из двух векторных слоёв; видимая зона — центральные 72 из 108 dp.
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(144.dp))
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(144.dp))
        }
        Text(
            stringResource(R.string.app_name), color = palette.text, fontSize = 22.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp),
        )
        Text(stringResource(R.string.about_version, version), color = palette.textMuted, fontSize = 14.sp)
        Text(
            stringResource(R.string.about_desc), color = palette.text, fontSize = 14.sp, textAlign = TextAlign.Center,
            lineHeight = 19.sp, modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
        )
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.cardBg),
    ) {
        AboutRow(stringResource(R.string.about_source), stringResource(R.string.about_source_value)) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.radio-browser.info")))
            }
        }
        AboutRow(stringResource(R.string.about_images), stringResource(R.string.about_images_value))
        AboutRow(stringResource(R.string.about_privacy), stringResource(R.string.about_privacy_value))
        AboutRow(stringResource(R.string.about_tech), stringResource(R.string.about_tech_value))
    }
}

@Composable
private fun AboutRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, color = palette.textMuted, fontSize = 12.sp)
        Text(value, color = if (onClick != null) palette.primary else palette.text, fontSize = 15.sp)
    }
}

/** Языки приложения: тег (как в locales_config.xml) и самоназвание языка. */
private val appLanguages = listOf(
    "en" to "English", "ru" to "Русский", "de" to "Deutsch", "fr" to "Français",
    "es" to "Español", "pt" to "Português", "it" to "Italiano", "pl" to "Polski", "tr" to "Türkçe",
    "nl" to "Nederlands", "ar" to "العربية", "hi" to "हिन्दी", "id" to "Bahasa Indonesia",
    "vi" to "Tiếng Việt", "ja" to "日本語", "ko" to "한국어", "zh-CN" to "中文（简体）",
)

/**
 * «Общие» → язык приложения. Смена — через AppCompat (per-app language): экран пересоздаётся на новом
 * языке, на Android 13+ выбор виден и в системных настройках приложения.
 */
@Composable
private fun GeneralTab() {
    val palette = LocalPalette.current
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val uiLocale = LocalLocale.current.platformLocale
    Section(stringResource(R.string.settings_language)) {
        Text(stringResource(R.string.settings_language_hint), color = palette.textMuted, fontSize = 12.sp, lineHeight = 15.sp)
        LanguageRow(
            title = stringResource(R.string.language_system), subtitle = null, selected = current.isEmpty(),
            onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) },
        )
        appLanguages.forEach { (tag, native) ->
            val selected = current.equals(tag, ignoreCase = true) ||
                (current.isNotEmpty() && Locale.forLanguageTag(current).language == Locale.forLanguageTag(tag).language &&
                    !current.contains('-') && !tag.contains('-'))
            LanguageRow(
                title = native,
                subtitle = Locale.forLanguageTag(tag).getDisplayName(uiLocale).replaceFirstChar { it.titlecase(uiLocale) },
                selected = selected,
                onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) },
            )
        }
    }
}

@Composable
private fun LanguageRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) palette.primary.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title, color = if (selected) palette.primary else palette.text, fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (subtitle != null && subtitle != title) Text(subtitle, color = palette.textMuted, fontSize = 12.sp)
        }
        if (selected) Icon(Icons.Filled.Check, null, tint = palette.primary)
    }
}
