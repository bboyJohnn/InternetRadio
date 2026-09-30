package com.tohn95.internetradio.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Папка избранного (как плейлист в Spotify). */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** Станция в папке. Одна станция может лежать в нескольких папках. */
@Entity(
    tableName = "folder_stations",
    primaryKeys = ["folderId", "stationUuid"],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class, parentColumns = ["id"], childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stationUuid")],
)
data class FolderStationEntity(
    val folderId: Long,
    val stationUuid: String,
    val addedAt: Long,
)
