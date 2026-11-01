// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.local.AppDatabase
import com.example.data.local.TrackEntity
import com.example.data.sample.SampleAudioGenerator
import com.example.model.AudioFormat
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Clean repository providing reactive data streams and zero-overhead media scanning.
 *
 * MEMORY & I/O OPTIMIZATION:
 * 1. Strict column projection in ContentResolver: loads only 7 primitive metadata fields.
 * 2. Cursors are closed immediately via use-blocks to avoid CursorWindow leaks.
 * 3. MediaStore operations execute entirely on Dispatchers.IO to maintain 60/120 FPS UI.
 */
class TrackRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val trackDao = database.trackDao()

    /**
     * Exposes all tracks ordered alphabetically.
     */
    val allTracks: Flow<List<Track>> = trackDao.getAllTracksFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    /**
     * Exposes tracks filtered strictly by audio format (MP3, WAV, FLAC, M4A, OGG).
     */
    fun getTracksByFormat(format: AudioFormat): Flow<List<Track>> =
        trackDao.getTracksByFormatFlow(format.name)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    /**
     * Exposes user's favorite tracks.
     */
    val favoriteTracks: Flow<List<Track>> = trackDao.getFavoriteTracksFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    /**
     * Exposes unique folders discovered on the device.
     */
    val folders: Flow<List<String>> = trackDao.getAllFoldersFlow()
        .flowOn(Dispatchers.IO)

    /**
     * Exposes tracks within a specific folder.
     */
    fun getTracksByFolder(folderName: String): Flow<List<Track>> =
        trackDao.getTracksByFolderFlow(folderName)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    /**
     * Toggles favorite status for a given track id.
     */
    suspend fun toggleFavorite(trackId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        trackDao.updateFavorite(trackId, isFavorite)
    }

    /**
     * Scans the system MediaStore for supported audio files (MP3, WAV, FLAC, M4A, OGG) without loading heavy metadata into RAM.
     *
     * MEMORY EFFICIENCY LOGIC:
     * - Uses a minimal projection array (only primitive IDs and text columns).
     * - Filters file extensions directly in SQL WHERE selection clause when possible,
     *   drastically reducing row iterations and heap allocations.
     */
    suspend fun scanDeviceAudio(): Int = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // Minimal column projection to prevent loading bloat
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Filter for music and supported formats (.mp3, .wav, .flac, .m4a, .aac, .ogg)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.mp3' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.wav' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.flac' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.m4a' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.aac' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.ogg'"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val scannedTracks = mutableListOf<TrackEntity>()

        try {
            resolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val path = cursor.getString(dataCol) ?: ""
                    val mime = cursor.getString(mimeCol)

                    val format = AudioFormat.fromPathOrMime(path, mime) ?: continue

                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val durationMs = cursor.getLong(durationCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol)

                    val file = File(path)
                    val folderName = file.parentFile?.name ?: "Internal Storage"
                    val cleanTitle = if (!rawTitle.isNullOrBlank() && rawTitle != "<unknown>") {
                        rawTitle
                    } else {
                        file.nameWithoutExtension.ifBlank { "Track #$id" }
                    }
                    val cleanArtist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") {
                        rawArtist
                    } else {
                        "Unknown Artist"
                    }

                    val contentUri = ContentUris.withAppendedId(collection, id).toString()

                    scannedTracks.add(
                        TrackEntity(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            durationMs = durationMs,
                            uriString = contentUri,
                            formatName = format.name,
                            folderName = folderName,
                            sizeBytes = sizeBytes,
                            isFavorite = false,
                            dateModified = dateModified
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Graceful fallback on permission error or restricted environment
        }

        if (scannedTracks.isNotEmpty()) {
            trackDao.insertAll(scannedTracks)
        } else {
            // If zero media found (fresh device or emulator), ensure sample tracks are generated
            val existingCount = trackDao.getTrackCount()
            if (existingCount == 0) {
                generateStarterSamples()
            }
        }

        scannedTracks.size
    }

    /**
     * Generates clean demo audio samples into app storage for immediate testing.
     */
    suspend fun generateStarterSamples(): List<Track> = withContext(Dispatchers.IO) {
        val samples = SampleAudioGenerator.generateStarterSamples(context)
        val entities = samples.map { TrackEntity.fromDomain(it) }
        trackDao.insertAll(entities)
        samples
    }

    /**
     * Imports a user-selected audio file or tree URI via Storage Access Framework.
     */
    suspend fun importAudioUri(uri: Uri, displayName: String, sizeBytes: Long): Track = withContext(Dispatchers.IO) {
        val format = AudioFormat.fromPathOrMime(displayName) ?: AudioFormat.MP3
        val id = uri.hashCode().toLong()

        val track = Track(
            id = id,
            title = displayName.substringBeforeLast(".").ifBlank { displayName },
            artist = "Local File",
            durationMs = 0L,
            uriString = uri.toString(),
            format = format,
            folderName = "Imported",
            sizeBytes = sizeBytes,
            isFavorite = false,
            dateModified = System.currentTimeMillis()
        )
        trackDao.insertAll(listOf(TrackEntity.fromDomain(track)))
        track
    }

    /**
     * Clears cached database entries.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        trackDao.clearAll()
    }
}
