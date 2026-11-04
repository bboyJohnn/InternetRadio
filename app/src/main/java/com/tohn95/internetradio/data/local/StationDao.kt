package com.tohn95.internetradio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Upsert suspend fun upsertAll(stations: List<StationEntity>)

    @Query("SELECT * FROM stations WHERE uuid = :uuid") suspend fun byUuid(uuid: String): StationEntity?

    @Query("SELECT * FROM stations WHERE uuid IN (:uuids)") suspend fun byUuids(uuids: List<String>): List<StationEntity>

    /**
     * Кэш свежей выдачи каталога, не затирая то, что сделал пользователь (избранное, история).
     * Одна транзакция и один запрос на пачку — раньше был отдельный запрос на каждую станцию,
     * и нажатое в это время сердечко могло перезаписаться старым значением.
     */
    @Transaction
    suspend fun cacheKeepingUserState(fresh: List<StationEntity>) {
        if (fresh.isEmpty()) return
        val known = fresh.map { it.uuid }.distinct().chunked(500).flatMap { byUuids(it) }.associateBy { it.uuid }
        upsertAll(fresh.map { e ->
            known[e.uuid]?.let { old ->
                e.copy(
                    isFavorite = old.isFavorite,
                    lastPlayedAt = old.lastPlayedAt,
                    favPosition = old.favPosition,
                    favoritedAt = old.favoritedAt,
                )
            } ?: e
        })
    }

    /**
     * Атомарное переключение избранного: read-modify-write в одной транзакции.
     * Room сериализует транзакции, поэтому два быстрых тапа по сердечку не «теряют обновление»
     * (второй видит уже зафиксированное значение). [candidate] — сущность для вставки,
     * если станции ещё нет в БД (уже с isFavorite=true, cachedAt=0).
     */
    @Transaction
    suspend fun toggleFavorite(candidate: StationEntity, now: Long = System.currentTimeMillis()) {
        val existing = byUuid(candidate.uuid)
        if (existing == null) {
            upsertAll(listOf(candidate.copy(isFavorite = true, favPosition = nextFavoritePosition(), favoritedAt = now)))
        } else if (existing.isFavorite) {
            setFavorite(candidate.uuid, false, existing.favPosition, existing.favoritedAt)
            // Убрали из избранного — убираем и из папок, чтобы станция не «воскресла» после повторного добавления.
            removeFromAllFolders(candidate.uuid)
        } else {
            setFavorite(candidate.uuid, true, nextFavoritePosition(), now)
        }
    }

    /** Пользовательская станция добавляется сверху, но повторное сохранение не снимает избранное. */
    @Transaction
    suspend fun upsertFavorite(candidate: StationEntity, now: Long = System.currentTimeMillis()) {
        val existing = byUuid(candidate.uuid)
        upsertAll(listOf(candidate.copy(
            isFavorite = true,
            favPosition = if (existing?.isFavorite == true) existing.favPosition else nextFavoritePosition(),
            favoritedAt = if (existing?.isFavorite == true) existing.favoritedAt else now,
            lastPlayedAt = existing?.lastPlayedAt,
        )))
    }

    @Query("SELECT COALESCE(MIN(favPosition), 0) - 1 FROM stations WHERE isFavorite = 1")
    suspend fun nextFavoritePosition(): Int

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY favPosition, name")
    fun favorites(): Flow<List<StationEntity>>

    @Query("SELECT uuid FROM stations WHERE isFavorite = 1")
    fun favoriteUuids(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM stations WHERE uuid = :uuid AND isFavorite = 1)")
    fun isFavorite(uuid: String): Flow<Boolean>

    @Query("UPDATE stations SET isFavorite = :fav, favPosition = :position, favoritedAt = :addedAt WHERE uuid = :uuid")
    suspend fun setFavorite(uuid: String, fav: Boolean, position: Int, addedAt: Long)

    @Query("SELECT * FROM stations WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 100")
    fun history(): Flow<List<StationEntity>>

    @Query("UPDATE stations SET lastPlayedAt = :at WHERE uuid = :uuid")
    suspend fun setPlayedAt(uuid: String, at: Long)

    @Query("SELECT * FROM stations WHERE cachedAt > 0 ORDER BY clickCount DESC LIMIT :limit")
    suspend fun cachedTopByClicks(limit: Int): List<StationEntity>

    @Query("SELECT * FROM stations WHERE cachedAt > 0 ORDER BY votes DESC LIMIT :limit")
    suspend fun cachedTopByVotes(limit: Int): List<StationEntity>

    // --- Папки избранного ---
    @Query("SELECT * FROM folders ORDER BY position, createdAt") fun folders(): Flow<List<FolderEntity>>
    @Query("SELECT * FROM folder_stations") fun folderLinks(): Flow<List<FolderStationEntity>>
    @Insert suspend fun insertFolder(folder: FolderEntity): Long
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM folders") suspend fun nextFolderPosition(): Int
    @Query("SELECT COALESCE(MIN(position), 0) - 1 FROM folder_stations WHERE folderId = :folderId")
    suspend fun nextFolderStationPosition(folderId: Long): Int

    @Transaction
    suspend fun appendFolder(folder: FolderEntity): Long =
        insertFolder(folder.copy(position = nextFolderPosition()))

    @Query("UPDATE folders SET name = :name WHERE id = :id") suspend fun renameFolder(id: Long, name: String)
    @Query("UPDATE folders SET coverPath = :path WHERE id = :id") suspend fun setFolderCover(id: Long, path: String?)
    @Query("DELETE FROM folders WHERE id = :id") suspend fun deleteFolderRow(id: Long)
    @Query("DELETE FROM folder_stations WHERE folderId = :id") suspend fun deleteFolderLinks(id: Long)
    @Query("SELECT folderId FROM folder_stations WHERE stationUuid = :uuid") suspend fun foldersOf(uuid: String): List<Long>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertLinkRows(links: List<FolderStationEntity>)

    @Transaction
    suspend fun insertLinksAtTop(links: List<FolderStationEntity>) {
        links.groupBy { it.folderId }.forEach { (folderId, rows) ->
            var position = nextFolderStationPosition(folderId)
            rows.forEach { row ->
                insertLinkRows(listOf(row.copy(position = position)))
                position--
            }
        }
    }

    @Query("DELETE FROM folder_stations WHERE folderId = :folderId AND stationUuid = :uuid")
    suspend fun removeFromFolder(folderId: Long, uuid: String)
    @Query("DELETE FROM folder_stations WHERE stationUuid = :uuid") suspend fun removeFromAllFolders(uuid: String)

    @Query("UPDATE stations SET favPosition = :position WHERE uuid = :uuid")
    suspend fun setFavoritePosition(uuid: String, position: Int)
    @Query("UPDATE folders SET position = :position WHERE id = :id")
    suspend fun setFolderPosition(id: Long, position: Int)
    @Query("UPDATE folder_stations SET position = :position WHERE folderId = :folderId AND stationUuid = :uuid")
    suspend fun setFolderStationPosition(folderId: Long, uuid: String, position: Int)

    @Transaction
    suspend fun reorderFavorites(orderedUuids: List<String>) {
        orderedUuids.forEachIndexed { index, uuid -> setFavoritePosition(uuid, index) }
    }

    @Transaction
    suspend fun reorderFolders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setFolderPosition(id, index) }
    }

    @Transaction
    suspend fun reorderFolder(folderId: Long, orderedUuids: List<String>) {
        orderedUuids.forEachIndexed { index, uuid -> setFolderStationPosition(folderId, uuid, index) }
    }

    /** Ссылки удаляем явно, не полагаясь на PRAGMA foreign_keys. Станции остаются в избранном. */
    @Transaction
    suspend fun deleteFolder(id: Long) {
        deleteFolderLinks(id)
        deleteFolderRow(id)
    }

    /** Состав папок для станции: снимаем лишние, новые добавляем наверх. */
    @Transaction
    suspend fun setMembership(uuid: String, folderIds: Set<Long>, now: Long) {
        val current = foldersOf(uuid).toSet()
        (current - folderIds).forEach { removeFromFolder(it, uuid) }
        insertLinksAtTop((folderIds - current).map { FolderStationEntity(it, uuid, now) })
    }
}
