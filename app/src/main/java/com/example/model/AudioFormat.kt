// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Enumeration of supported lightweight audio formats.
 * Designed to cleanly segment music files with refined, sober visual accents (no neon).
 */
enum class AudioFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val accentColor: Color
) {
    MP3("mp3", "audio/mpeg", "MP3", Color(0xFFCC8538)),
    WAV("wav", "audio/wav", "WAV", Color(0xFF174C98)),
    FLAC("flac", "audio/flac", "FLAC", Color(0xFF2E7D32)),
    M4A("m4a", "audio/mp4", "M4A", Color(0xFF7E72A6)),
    OGG("ogg", "audio/ogg", "OGG", Color(0xFFD84315));

    companion object {
        /**
         * Resolves the AudioFormat from a filename extension or MIME type.
         * Returns null if the format is unsupported, preventing unwanted file indexing.
         */
        fun fromPathOrMime(pathOrName: String, mimeType: String? = null): AudioFormat? {
            val lower = pathOrName.lowercase()
            return when {
                lower.endsWith(".mp3") || mimeType?.contains("mpeg", ignoreCase = true) == true -> MP3
                lower.endsWith(".wav") || mimeType?.contains("wav", ignoreCase = true) == true -> WAV
                lower.endsWith(".flac") || mimeType?.contains("flac", ignoreCase = true) == true -> FLAC
                lower.endsWith(".m4a") || lower.endsWith(".aac") || mimeType?.contains("mp4", ignoreCase = true) == true || mimeType?.contains("aac", ignoreCase = true) == true -> M4A
                lower.endsWith(".ogg") || mimeType?.contains("ogg", ignoreCase = true) == true -> OGG
                else -> null
            }
        }
    }
}
