// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.AudioFormat
import com.example.model.Track

/**
 * Room database entity for indexed local audio tracks.
 *
 * MEMORY OPTIMIZATION NOTE:
 * Explicit indices on 'format_name' and 'is_favorite' avoid full table scans in SQLite,
 * minimizing disk I/O and query execution times on low-end hardware.
 */
@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["format_name"]),
        Index(value = ["is_favorite"]),
        Index(value = ["folder_name"])
    ]
)
data class TrackEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist")
    val artist: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "uri_string")
    val uriString: String,

    @ColumnInfo(name = "format_name")
    val formatName: String,

    @ColumnInfo(name = "folder_name")
    val folderName: String,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "date_modified")
    val dateModified: Long = 0L
) {
    /**
     * Converts database entity to the lightweight domain model.
     */
    fun toDomain(): Track {
        val format = try {
            AudioFormat.valueOf(formatName)
        } catch (_: Exception) {
            AudioFormat.MP3
        }
        return Track(
            id = id,
            title = title,
            artist = artist,
            durationMs = durationMs,
            uriString = uriString,
            format = format,
            folderName = folderName,
            sizeBytes = sizeBytes,
            isFavorite = isFavorite,
            dateModified = dateModified
        )
    }

    companion object {
        fun fromDomain(track: Track): TrackEntity {
            return TrackEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                durationMs = track.durationMs,
                uriString = track.uriString,
                formatName = track.format.name,
                folderName = track.folderName,
                sizeBytes = track.sizeBytes,
                isFavorite = track.isFavorite,
                dateModified = track.dateModified
            )
        }
    }
}
