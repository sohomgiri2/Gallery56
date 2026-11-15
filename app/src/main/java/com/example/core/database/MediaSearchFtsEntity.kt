package com.example.core.database

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = MediaIndexEntity::class)
@Entity(tableName = "media_search_fts")
data class MediaSearchFtsEntity(
    val fileName: String,
    val displayName: String,
    val albumName: String,
    val relativePath: String,
    val format: String,
    val cameraMake: String,
    val cameraModel: String,
    val ocrText: String,
    val aiLabelsText: String,
    val aiObjectsText: String,
    val generatedCaption: String,
    val userTags: String,
    val userNotes: String,
)
