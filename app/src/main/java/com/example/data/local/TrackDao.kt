// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local track entities.
 *
 * MEMORY & THREADING OPTIMIZATION:
 * All stream queries return Flow with automatic asynchronous SQLite observation.
 * Write operations run within transactions on background dispatchers to guarantee
 * zero main-thread stall (ANR prevention).
 */
@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun getAllTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE format_name = :formatName ORDER BY title COLLATE NOCASE ASC")
    fun getTracksByFormatFlow(formatName: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavoriteTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT DISTINCT folder_name FROM tracks ORDER BY folder_name COLLATE NOCASE ASC")
    fun getAllFoldersFlow(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE folder_name = :folderName ORDER BY title COLLATE NOCASE ASC")
    fun getTracksByFolderFlow(folderName: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int

    @Query("SELECT COUNT(*) FROM tracks WHERE format_name = :formatName")
    suspend fun getCountByFormat(formatName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("UPDATE tracks SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM tracks")
    suspend fun clearAll()

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
