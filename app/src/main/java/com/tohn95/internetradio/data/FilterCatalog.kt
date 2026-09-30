package com.tohn95.internetradio.data

import com.tohn95.internetradio.data.remote.RadioBrowserClient
import com.tohn95.internetradio.domain.model.Country
import com.tohn95.internetradio.domain.model.TagOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Справочники фильтров: один успешный запрос на процесс; ошибка -> emptyList,
 *  при следующем вызове пробуем снова. */
@Singleton
class FilterCatalog @Inject constructor(private val api: RadioBrowserClient) {
    private val mutex = Mutex()
    private var countriesCache: List<Country>? = null
    private var tagsCache: List<TagOption>? = null
    private var codecsCache: List<TagOption>? = null

    suspend fun countries(): List<Country> = mutex.withLock {
        countriesCache ?: runCatching { api.countries() }.getOrNull()?.also { countriesCache = it } ?: emptyList()
    }

    suspend fun tags(): List<TagOption> = mutex.withLock {
        tagsCache ?: runCatching { api.tags() }.getOrNull()?.also { tagsCache = it } ?: emptyList()
    }

    suspend fun codecs(): List<TagOption> = mutex.withLock {
        codecsCache ?: runCatching { api.codecs() }.getOrNull()?.also { codecsCache = it } ?: emptyList()
    }
}
