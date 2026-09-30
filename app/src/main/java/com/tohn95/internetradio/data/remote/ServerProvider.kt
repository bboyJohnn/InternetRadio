package com.tohn95.internetradio.data.remote

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import javax.inject.Singleton

/**
 * Выбор сервера radio-browser: DNS all.api.radio-browser.info -> имена зеркал; при провале — fallback-список.
 * Найденный список запоминаем на диске и отдаём сразу, а обновляем раз в сутки в фоне: обратный DNS
 * медленный, и раньше он задерживал самый первый запрос каждого запуска. context null в юнит-тестах.
 */
@Singleton
class ServerProvider constructor(private val context: Context?) {
    @Volatile private var cached: Pair<Long, List<String>>? = null
    @Volatile private var refreshing = false
    private val prefs by lazy { context?.getSharedPreferences("radio_browser_servers", Context.MODE_PRIVATE) }
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun baseUrls(): List<String> {
        val now = System.currentTimeMillis()
        (cached ?: saved()?.also { cached = it })?.let { (at, urls) ->
            if (now - at >= TTL_MS) refreshInBackground()
            return urls
        }
        // Самый первый запуск: ждём поиска зеркал (он ограничен по времени), иначе — запасной список.
        val found = withContext(Dispatchers.IO) { discover() }
        return if (found != null) {
            store(now, found)
            found
        } else {
            cached = (now - TTL_MS + RETRY_MS) to FALLBACK      // запасной ненадолго: попробуем найти снова
            FALLBACK
        }
    }

    private fun refreshInBackground() {
        if (refreshing) return
        refreshing = true
        background.launch {
            try {
                discover()?.let { store(System.currentTimeMillis(), it) }
            } finally {
                refreshing = false
            }
        }
    }

    /** Имена зеркал: обратный DNS для всех адресов параллельно, не дольше [LOOKUP_TIMEOUT_MS]. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun discover(): List<String>? = runCatching {
        val addresses = InetAddress.getAllByName("all.api.radio-browser.info")
        val lookups = addresses.map { a -> background.async { a.canonicalHostName.takeIf { it != a.hostAddress } } }
        withTimeoutOrNull(LOOKUP_TIMEOUT_MS) { lookups.awaitAll() }
        lookups.filter { it.isCompleted && !it.isCancelled }.mapNotNull { runCatching { it.getCompleted() }.getOrNull() }
            .distinct().shuffled().map { "https://$it/" }
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    private fun saved(): Pair<Long, List<String>>? {
        val p = prefs ?: return null
        val urls = p.getString(KEY_URLS, null)?.split('\n')?.filter { it.startsWith("https://") }.orEmpty()
        return if (urls.isEmpty()) null else p.getLong(KEY_AT, 0L) to urls
    }

    private fun store(at: Long, urls: List<String>) {
        cached = at to urls
        prefs?.edit { putString(KEY_URLS, urls.joinToString("\n")); putLong(KEY_AT, at) }
    }

    companion object {
        val FALLBACK = listOf(
            "https://de1.api.radio-browser.info/",
            "https://de2.api.radio-browser.info/",
        )
        private const val TTL_MS = 24 * 3600_000L
        private const val RETRY_MS = 5 * 60_000L
        private const val LOOKUP_TIMEOUT_MS = 3_000L
        private const val KEY_URLS = "urls"
        private const val KEY_AT = "at"
    }
}
