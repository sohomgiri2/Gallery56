package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "face_groups")
data class FaceGroupEntity(
    @PrimaryKey val groupId: String,
    val label: String = "",
    val isUserNamed: Boolean = false,
    val coverCropPath: String? = null,
    val faceCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "face_detections",
    indices = [
        Index(value = ["mediaStoreId"]),
        Index(value = ["groupId"]),
    ],
)
data class FaceDetectionEntity(
    @PrimaryKey val faceId: String,
    val mediaStoreId: Long,
    val groupId: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val featureVector: String,
    val smilingProbability: Float,
    val leftEyeOpenProbability: Float,
    val rightEyeOpenProbability: Float,
    val headEulerY: Float,
    val headEulerZ: Float,
    val cropPath: String? = null,
)
