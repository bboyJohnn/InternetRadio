package com.tohn95.internetradio.data

import com.tohn95.internetradio.data.local.FolderEntity
import com.tohn95.internetradio.data.local.FolderStationEntity
import com.tohn95.internetradio.data.local.StationDao
import com.tohn95.internetradio.data.local.toDomain
import com.tohn95.internetradio.data.local.toEntity
import com.tohn95.internetradio.data.remote.RadioBrowserClient
import com.tohn95.internetradio.domain.model.Folder
import com.tohn95.internetradio.domain.model.HistoryEntry
import com.tohn95.internetradio.domain.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

data class CatalogResult(val stations: List<Station>, val fromCache: Boolean)

enum class VoteOutcome { OK, ALREADY, ERROR }

/** Случайная выборка без повторов; вынесена отдельно, чтобы тестировать с заданным Random. */
fun randomPick(pool: List<Station>, take: Int, random: Random): List<Station> =
    pool.shuffled(random).take(take)

@Singleton
class StationRepository @Inject constructor(
    private val api: RadioBrowserClient,
    private val dao: StationDao,
    private val logos: LogoOverrides,
) {
    // Выбранные пользователем логотипы подставляются во все выдачи; смена логотипа сразу обновляет списки.
    fun favorites(): Flow<List<Station>> = combine(dao.favorites(), logos.map) { l, _ -> logos.apply(l.map { it.toDomain() }) }
    fun history(): Flow<List<HistoryEntry>> = combine(dao.history(), logos.map) { l, _ ->
        l.map { HistoryEntry(logos.apply(it.toDomain()), it.lastPlayedAt ?: 0L) }
    }

    /** Запомнить выбранный логотип станции. */
    fun setLogo(uuid: String, url: String) = logos.set(uuid, url)
    fun withLogo(station: Station): Station = logos.apply(station)
    fun isFavorite(uuid: String): Flow<Boolean> = dao.isFavorite(uuid)
    fun favoriteUuids(): Flow<Set<String>> = dao.favoriteUuids().map { it.toSet() }

    // Атомарно в DAO (@Transaction) — устойчиво к гонке двойного тапа по сердечку в списках.
    suspend fun toggleFavorite(station: Station) =
        dao.toggleFavorite(station.toEntity(cachedAt = 0, isFavorite = true))

    /** Станции, за которые голосовали в этом запуске (radio-browser всё равно пускает не чаще раза в 10 мин). */
    private val _voted = MutableStateFlow<Set<String>>(emptySet())
    val votedUuids: StateFlow<Set<String>> = _voted

    /** Голос за станцию в каталоге radio-browser. */
    suspend fun vote(uuid: String): VoteOutcome = try {
        val r = api.vote(uuid)
        when {
            r.ok -> { _voted.value = _voted.value + uuid; VoteOutcome.OK }
            r.message.contains("often", ignoreCase = true) -> { _voted.value = _voted.value + uuid; VoteOutcome.ALREADY }
            else -> VoteOutcome.ERROR
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        VoteOutcome.ERROR
    }

    /** Своя станция по ссылке: сразу в Избранное (cachedAt=0 — в офлайн-выдачу каталога не попадает). */
    suspend fun addCustomStation(station: Station) =
        dao.upsertAll(listOf(station.toEntity(cachedAt = 0, isFavorite = true)))

    /** ♡ из уведомления: там есть только uuid; станция уже в базе (её записала история при запуске). */
    suspend fun toggleFavoriteByUuid(uuid: String) {
        dao.byUuid(uuid)?.let { dao.toggleFavorite(it) }
    }

    /** Папки со станциями из избранного; станция вне избранного в папке не показывается. */
    fun folders(): Flow<List<Folder>> = combine(dao.folders(), dao.folderLinks(), favorites()) { folders, links, favs ->
        val byUuid = favs.associateBy { it.uuid }
        val linksByFolder = links.groupBy { it.folderId }
        folders.map { f ->
            Folder(
                f.id, f.name,
                linksByFolder[f.id].orEmpty().sortedByDescending { it.addedAt }.mapNotNull { byUuid[it.stationUuid] },
            )
        }
    }

    suspend fun createFolder(name: String): Long =
        dao.insertFolder(FolderEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    suspend fun renameFolder(id: Long, name: String) = dao.renameFolder(id, name.trim())
    suspend fun deleteFolder(id: Long) = dao.deleteFolder(id)
    suspend fun removeFromFolder(folderId: Long, uuid: String) = dao.removeFromFolder(folderId, uuid)
    suspend fun addToFolder(folderId: Long, uuid: String) =
        dao.insertLinks(listOf(FolderStationEntity(folderId, uuid, System.currentTimeMillis())))
    suspend fun setFolders(uuid: String, folderIds: Set<Long>) =
        dao.setMembership(uuid, folderIds, System.currentTimeMillis())

    suspend fun recordPlayed(station: Station) {
        if (dao.byUuid(station.uuid) == null)
            dao.upsertAll(listOf(station.toEntity(cachedAt = 0)))
        dao.setPlayedAt(station.uuid, System.currentTimeMillis())
    }

    // Не затираем isFavorite/lastPlayedAt у знакомых станций — это делает DAO одной транзакцией.
    private suspend fun cacheAll(stations: List<Station>) {
        val now = System.currentTimeMillis()
        dao.cacheKeepingUserState(stations.distinctBy { it.uuid }.map { it.toEntity(cachedAt = now) })
    }

    // Офлайн-фолбэк сознательно отдаёт кэш любого возраста: старый список лучше пустого экрана.
    private suspend fun fetchOrCache(
        remote: suspend () -> List<Station>,
        cached: suspend () -> List<Station>,
    ): CatalogResult = try {
        val fresh = remote()
        cacheAll(fresh)
        CatalogResult(logos.apply(fresh), fromCache = false)
    } catch (e: CancellationException) {
        throw e                 // экран закрыли / запрос сменился — это не «нет сети», кэш не нужен
    } catch (e: Exception) {
        val fallback = cached()
        if (fallback.isEmpty()) throw e
        CatalogResult(logos.apply(fallback), fromCache = true)
    }

    suspend fun topByClicks(limit: Int, offset: Int): CatalogResult = fetchOrCache(
        remote = { api.topByClicks(limit, offset) },
        cached = { dao.cachedTopByClicks(limit).map { it.toDomain() } },
    )

    suspend fun topByVotes(limit: Int, offset: Int): CatalogResult = fetchOrCache(
        remote = { api.topByVotes(limit, offset) },
        cached = { dao.cachedTopByVotes(limit).map { it.toDomain() } },
    )

    /** Пул популярных (по кликам) + случайная выборка из него — «Популярные» обновляются по-новому. */
    suspend fun randomPopular(
        poolSize: Int = 300,
        take: Int = 30,
        random: Random = Random.Default,
    ): CatalogResult = fetchOrCache(
        remote = { randomPick(api.topByClicks(poolSize, 0), take, random) },
        cached = { randomPick(dao.cachedTopByClicks(poolSize).map { it.toDomain() }, take, random) },
    )

    suspend fun searchStations(
        name: String?, countryCode: String?, tag: String?, language: String?,
        codec: String? = null, bitrateMin: Int? = null,
        order: String = "clickcount", reverse: Boolean = true,
        limit: Int, offset: Int,
        bitrateMax: Int? = null,
    ): CatalogResult = fetchOrCache(
        remote = { api.searchStations(name, countryCode, tag, language, codec, bitrateMin, order, reverse, limit, offset, bitrateMax) },
        cached = { emptyList() },   // офлайн-поиск не поддерживаем
    )

    /** Подборка для списков Главной/жанров: те же фильтры, что в поиске, без имени. */
    suspend fun browse(
        tag: String? = null, countryCode: String? = null,
        name: String? = null,
        bitrateMin: Int? = null, bitrateMax: Int? = null,
        order: String = "clickcount", reverse: Boolean = true,
        limit: Int, offset: Int = 0,
    ): List<Station> = api.searchStations(
        name = name, countryCode = countryCode, tag = tag, language = null,
        bitrateMin = bitrateMin, bitrateMax = bitrateMax,
        order = order, reverse = reverse, limit = limit, offset = offset,
    ).distinctBy { it.uuid }.let(logos::apply)

    /**
     * «Новая станция»: самая свежая запись каталога (order=changetimestamp) с логотипом и хоть
     * одним слушателем. Если таких нет — станция из середины популярных.
     */
    suspend fun newStation(random: Random = Random.Default): Station? {
        val fresh = try {
            browse(order = "changetimestamp", limit = 150)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }.filter { !it.faviconUrl.isNullOrBlank() && it.clickCount > 0 }
        if (fresh.isNotEmpty()) return fresh.take(8).random(random)
        return browse(order = "clickcount", limit = 100, offset = 150).randomOrNull(random)
    }
}
