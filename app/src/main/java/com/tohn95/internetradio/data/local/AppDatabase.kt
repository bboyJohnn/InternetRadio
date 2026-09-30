package com.tohn95.internetradio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StationEntity::class, FolderEntity::class, FolderStationEntity::class],
    version = 3,
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
    }
}
