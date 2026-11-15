package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceDao {

    @Query("SELECT * FROM face_groups ORDER BY isUserNamed DESC, faceCount DESC, createdAtMillis ASC")
    fun observeAllGroups(): Flow<List<FaceGroupEntity>>

    @Query("SELECT * FROM face_groups")
    suspend fun getAllGroupsSnapshot(): List<FaceGroupEntity>

    @Query("SELECT * FROM face_groups WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): FaceGroupEntity?

    @Query("SELECT * FROM face_detections ORDER BY mediaStoreId DESC")
    fun observeAllFaces(): Flow<List<FaceDetectionEntity>>

    @Query("SELECT * FROM face_detections WHERE mediaStoreId = :mediaStoreId")
    suspend fun getFacesForMedia(mediaStoreId: Long): List<FaceDetectionEntity>

    @Query("SELECT * FROM face_detections WHERE groupId = :groupId")
    suspend fun getFacesInGroup(groupId: String): List<FaceDetectionEntity>

    @Query("SELECT DISTINCT mediaStoreId FROM face_detections WHERE groupId IN (:groupIds)")
    suspend fun getMediaIdsForGroups(groupIds: List<String>): List<Long>

    @Query("SELECT * FROM face_detections")
    suspend fun getAllFacesSnapshot(): List<FaceDetectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(groups: List<FaceGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: FaceGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFaces(faces: List<FaceDetectionEntity>)

    @Query("UPDATE face_groups SET label = :label, isUserNamed = :isUserNamed WHERE groupId = :groupId")
    suspend fun renameGroup(groupId: String, label: String, isUserNamed: Boolean)

    @Query("UPDATE face_detections SET groupId = :targetGroupId WHERE groupId = :sourceGroupId")
    suspend fun reassignGroupFaces(sourceGroupId: String, targetGroupId: String)

    @Query("UPDATE face_detections SET groupId = :targetGroupId WHERE faceId = :faceId")
    suspend fun moveFaceToGroup(faceId: String, targetGroupId: String)

    @Query("DELETE FROM face_detections WHERE faceId = :faceId")
    suspend fun deleteFace(faceId: String)

    @Query("DELETE FROM face_detections WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteFacesForMedia(mediaStoreId: Long)

    @Query("DELETE FROM face_detections WHERE groupId = :groupId")
    suspend fun deleteFacesInGroup(groupId: String)

    @Query("DELETE FROM face_groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM face_detections")
    suspend fun deleteAllFaces()

    @Query("DELETE FROM face_groups")
    suspend fun deleteAllGroups()
}
