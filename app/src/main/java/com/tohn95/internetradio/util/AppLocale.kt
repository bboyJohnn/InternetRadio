package com.tohn95.internetradio.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

/**
 * Контекст на языке приложения (выбран в Настройках → Общие). Нужен вне активности — сервис, уведомление:
 * на Android 12 и ниже AppCompat переводит только активности, а строки сервиса остались бы на языке системы.
 */
fun Context.withAppLocale(): Context {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return this
    val config = Configuration(resources.configuration).apply { setLocales(LocaleList.forLanguageTags(locales.toLanguageTags())) }
    return createConfigurationContext(config)
}
