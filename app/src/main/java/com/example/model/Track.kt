// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.model

import java.util.Locale

/**
 * Lightweight immutable representation of an audio track.
 *
 * MEMORY OPTIMIZATION NOTE:
 * Deliberately excludes embedded album art byte arrays, ID3 picture frames, and heavy metadata objects.
 * Uses only primitive strings and longs to maintain a sub-kilobyte memory footprint per track instance.
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uriString: String,
    val format: AudioFormat,
    val folderName: String,
    val sizeBytes: Long,
    val isFavorite: Boolean = false,
    val dateModified: Long = 0L
) {
    /**
     * Formats duration into mm:ss or hh:mm:ss without allocating heavy date-time formatters.
     */
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val remainingSeconds = totalSeconds % 60
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, remainingMinutes, remainingSeconds)
            } else {
                String.format("%02d:%02d", remainingMinutes, remainingSeconds)
            }
        }

    /**
     * Formats file size into KB or MB with minimal string allocations.
     */
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 KB"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format(Locale.US, "%.1f MB", mb)
            } else {
                String.format(Locale.US, "%.0f KB", kb)
            }
        }
}
