package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_searches")
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val summary: String,
    val queryJson: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastUsedMillis: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
)
