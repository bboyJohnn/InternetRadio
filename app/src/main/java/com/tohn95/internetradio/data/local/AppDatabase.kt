package com.tohn95.internetradio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StationEntity::class, FolderEntity::class, FolderStationEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        /** 1→2: три новых поля станции. Избранное и история обязаны пережить обновление. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stations ADD COLUMN language TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stations ADD COLUMN homepage TEXT")
                db.execSQL("ALTER TABLE stations ADD COLUMN lastCheckOk INTEGER NOT NULL DEFAULT 1")
            }
        }

        /** 2→3: папки избранного. Сами станции и флаг isFavorite не трогаем — избранное сохраняется. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folder_stations` (`folderId` INTEGER NOT NULL, " +
                        "`stationUuid` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`folderId`, `stationUuid`), FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_folder_stations_stationUuid` ON `folder_stations` (`stationUuid`)")
            }
        }

        /** 3→4: сохраняем прежние порядки, добавляем пользовательские позиции и обложки. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stations ADD COLUMN favPosition INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stations ADD COLUMN favoritedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE folders ADD COLUMN coverPath TEXT")
                db.execSQL("ALTER TABLE folder_stations ADD COLUMN position INTEGER NOT NULL DEFAULT 0")

                var position = 0
                db.query("SELECT uuid FROM stations WHERE isFavorite = 1 ORDER BY name").use { cursor ->
                    while (cursor.moveToNext()) {
                        db.execSQL(
                            "UPDATE stations SET favPosition = ?, favoritedAt = ? WHERE uuid = ?",
                            arrayOf(position, position.toLong(), cursor.getString(0)),
                        )
                        position++
                    }
                }

                val folderIds = mutableListOf<Long>()
                db.query("SELECT id FROM folders ORDER BY createdAt").use { cursor ->
                    while (cursor.moveToNext()) folderIds += cursor.getLong(0)
                }
                folderIds.forEachIndexed { index, id ->
                    db.execSQL("UPDATE folders SET position = ? WHERE id = ?", arrayOf<Any>(index, id))
                    var stationPosition = 0
                    db.query(
                        "SELECT stationUuid FROM folder_stations WHERE folderId = ? ORDER BY addedAt DESC",
                        arrayOf(id),
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            db.execSQL(
                                "UPDATE folder_stations SET position = ? WHERE folderId = ? AND stationUuid = ?",
                                arrayOf(stationPosition, id, cursor.getString(0)),
                            )
                            stationPosition++
                        }
                    }
                }
            }
        }
    }
}
