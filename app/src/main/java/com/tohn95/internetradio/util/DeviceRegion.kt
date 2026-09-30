package com.tohn95.internetradio.util

import android.content.Context
import android.content.res.Resources
import android.telephony.TelephonyManager

/**
 * Страна пользователя (ISO, «RU») для секции «Эфир»: регион системы, иначе SIM, иначе сеть оператора.
 * Не из языка приложения: выбранный в «Общих» язык («ru», «en») страны не содержит — секция пропадала.
 */
fun Context.deviceCountryCode(): String {
    val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val candidates = sequenceOf(
        { Resources.getSystem().configuration.locales[0]?.country },
        { tm?.simCountryIso },
        { tm?.networkCountryIso },
    )
    return candidates.mapNotNull { runCatching(it).getOrNull() }
        .firstOrNull { it.length == 2 }?.uppercase().orEmpty()
}
