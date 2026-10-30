// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.player

import com.example.model.Track

/**
 * Repeat modes for the playback queue.
 */
enum class RepeatMode {
    OFF,
    REPEAT_ALL,
    REPEAT_ONE
}

/**
 * Immutable state representing current audio playback status.
 */
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Progress ratio from 0.0f to 1.0f for seek bars.
     */
    val progressFraction: Float
        get() {
            val d = durationMs.coerceAtLeast(1L)
            return (currentPositionMs.toFloat() / d.toFloat()).coerceIn(0f, 1f)
        }

    /**
     * Human-readable current position (mm:ss).
     */
    val formattedPosition: String
        get() = formatTime(currentPositionMs)

    /**
     * Human-readable total duration (mm:ss).
     */
    val formattedDuration: String
        get() = formatTime(durationMs)

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val hrs = min / 60
        val remMin = min % 60
        return if (hrs > 0) {
            String.format("%d:%02d:%02d", hrs, remMin, sec)
        } else {
            String.format("%02d:%02d", remMin, sec)
        }
    }
}
