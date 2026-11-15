package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    version = 2,
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

        fun getInstance(context: Context): Gallery56Database {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    Gallery56Database::class.java,
                    "gallery56_index.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
