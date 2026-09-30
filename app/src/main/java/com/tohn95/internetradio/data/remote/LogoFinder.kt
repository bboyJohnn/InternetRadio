package com.tohn95.internetradio.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Поиск логотипа станции на её сайте (идея radioMii, MIT): собираем кандидатов из HTML
 * (og:image, apple-touch-icon, иконки манифеста, <link rel=icon>, msapplication-TileImage) и
 * стандартных путей (/apple-touch-icon.png, /favicon.ico), ранжируем и проверяем, что это картинка.
 * SVG пропускаем — Coil без отдельного модуля их не рисует.
 */
@Singleton
class LogoFinder @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private data class Candidate(val url: HttpUrl, val priority: Int, val size: Int)

    suspend fun find(homepage: String, max: Int = 8): List<String> = withContext(Dispatchers.IO) {
        val page = normalize(homepage) ?: return@withContext emptyList()
        val origin = page.newBuilder().encodedPath("/").query(null).fragment(null).build()
        val html = fetchText(page, limit = 600_000).orEmpty()
        val base = baseHref(html, page) ?: page

        val found = mutableListOf<Candidate>()
        fun add(raw: String?, priority: Int, size: Int = 0) {
            val u = raw?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("data:") }?.let { base.resolve(it) } ?: return
            if (u.encodedPath.endsWith(".svg", ignoreCase = true)) return
            found += Candidate(u, priority, size)
        }

        for (tag in TAG.findAll(html)) {
            val name = tag.groupValues[1].lowercase()
            val a = attrs(tag.value)
            when (name) {
                "meta" -> {
                    val key = (a["property"] ?: a["name"] ?: a["itemprop"])?.lowercase()
                    when (key) {
                        "og:image", "og:image:url", "og:image:secure_url" -> add(a["content"], 50)
                        "msapplication-tileimage" -> add(a["content"], 35)
                        "twitter:image", "twitter:image:src" -> add(a["content"], 30)
                        "image", "logo" -> add(a["content"], 25)
                    }
                }
                "link" -> {
                    val rel = a["rel"]?.lowercase().orEmpty()
                    val size = a["sizes"]?.substringBefore('x')?.toIntOrNull() ?: 0
                    when {
                        "apple-touch-icon" in rel -> add(a["href"], 40, size)
                        "manifest" in rel -> manifestIcons(a["href"]?.let { base.resolve(it) }).forEach { (u, s) -> add(u, 32, s) }
                        rel.split(' ').contains("icon") -> add(a["href"], 10, size)
                    }
                }
            }
        }
        add(origin.resolve("/apple-touch-icon.png")?.toString(), 38, 180)
        add(origin.resolve("/favicon.ico")?.toString(), 5, 32)

        val ranked = found.distinctBy { it.url.toString() }
            .sortedWith(compareByDescending<Candidate> { it.priority }.thenByDescending { it.size })
            .take(max * 2)
        // Проверяем параллельно, что по ссылке действительно картинка; порядок — по рангу.
        coroutineScope { ranked.map { c -> async { c.url.toString().takeIf { isImage(c.url) } } }.awaitAll() }
            .filterNotNull().take(max)
    }

    private fun normalize(homepage: String): HttpUrl? {
        val h = homepage.trim()
        return (if (h.startsWith("http", ignoreCase = true)) h else "https://$h").toHttpUrlOrNull()
    }

    private fun baseHref(html: String, page: HttpUrl): HttpUrl? =
        Regex("""<base\s[^>]*href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
            ?.groupValues?.get(1)?.let { page.resolve(it) }

    /** Иконки из web-манифеста: [(src, размер)]. */
    private fun manifestIcons(url: HttpUrl?): List<Pair<String, Int>> {
        url ?: return emptyList()
        val text = fetchText(url, limit = 200_000) ?: return emptyList()
        return runCatching {
            val icons = json.parseToJsonElement(text).jsonObject["icons"] as? JsonArray ?: return emptyList()
            icons.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val src = o["src"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val size = o["sizes"]?.jsonPrimitive?.content?.substringBefore('x')?.toIntOrNull() ?: 0
                url.resolve(src)?.toString()?.let { it to size }
            }
        }.getOrDefault(emptyList())
    }

    private fun fetchText(url: HttpUrl, limit: Long): String? = runCatching {
        client.newCall(Request.Builder().url(url).header("User-Agent", UA).build()).execute().use { r ->
            if (!r.isSuccessful) return null
            val body = r.body.source()
            body.request(limit)
            body.buffer.clone().readUtf8(minOf(limit, body.buffer.size))
        }
    }.getOrNull()

    private fun isImage(url: HttpUrl): Boolean = runCatching {
        client.newCall(Request.Builder().url(url).header("User-Agent", UA).build()).execute().use { r ->
            val type = r.header("Content-Type").orEmpty().lowercase()
            val len = r.header("Content-Length")?.toLongOrNull()
            r.isSuccessful && type.startsWith("image/") && "svg" !in type && (len == null || len > 200)
        }
    }.getOrDefault(false)

    private fun attrs(tag: String): Map<String, String> =
        ATTR.findAll(tag).associate { m ->
            m.groupValues[1].lowercase() to m.groupValues[2].trim('"', '\'').replace("&amp;", "&")
        }

    private companion object {
        val TAG = Regex("""<(meta|link)\b[^>]*>""", RegexOption.IGNORE_CASE)
        val ATTR = Regex("""([\w:-]+)\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)""")
        const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
