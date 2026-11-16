package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MediaIndexEntity::class,
        MediaSearchFtsEntity::class,
        FaceGroupEntity::class,
        FaceDetectionEntity::class,
        SavedSearchEntity::class,
        CustomAlbumEntity::class,
        CustomAlbumMemberEntity::class,
        VaultItemEntity::class,
        TrashItemEntity::class,
        VideoPlaybackEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class Gallery56Database : RoomDatabase() {
    abstract fun mediaIndexDao(): MediaIndexDao
    abstract fun faceDao(): FaceDao
    abstract fun savedSearchDao(): SavedSearchDao
    abstract fun customAlbumDao(): CustomAlbumDao
    abstract fun vaultDao(): VaultDao
    abstract fun trashDao(): TrashDao
    abstract fun videoPlaybackDao(): VideoPlaybackDao

    companion object {
        @Volatile
        private var instance: Gallery56Database? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE media_index ADD COLUMN lastViewedAtMillis INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE media_index ADD COLUMN lastEditedAtMillis INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE custom_albums ADD COLUMN colorLabelHex TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE custom_albums ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        fun getInstance(context: Context): Gallery56Database {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    Gallery56Database::class.java,
                    "gallery56_index.db",
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
