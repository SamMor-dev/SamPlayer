// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example

import com.example.model.AudioFormat
import com.example.model.Track
import com.example.player.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAudioFormatResolution() {
        assertEquals(AudioFormat.MP3, AudioFormat.fromPathOrMime("song.mp3"))
        assertEquals(AudioFormat.WAV, AudioFormat.fromPathOrMime("audio.wav"))
        assertEquals(AudioFormat.MIDI, AudioFormat.fromPathOrMime("theme.mid"))
        assertEquals(AudioFormat.MIDI, AudioFormat.fromPathOrMime("track.midi"))
        assertEquals(AudioFormat.MP3, AudioFormat.fromPathOrMime("test", "audio/mpeg"))
        assertNull(AudioFormat.fromPathOrMime("image.png"))
    }

    @Test
    fun testTrackFormatting() {
        val track = Track(
            id = 1L,
            title = "Test Track",
            artist = "Artist",
            durationMs = 125000L, // 2 mins 5 secs
            uriString = "content://audio/1",
            format = AudioFormat.MP3,
            folderName = "Music",
            sizeBytes = 3 * 1024 * 1024L // 3 MB
        )

        assertEquals("02:05", track.formattedDuration)
        assertEquals("3.0 MB", track.formattedSize)
    }

    @Test
    fun testPlaybackStateProgress() {
        val state = PlaybackState(
            currentPositionMs = 30000L,
            durationMs = 60000L
        )
        assertEquals(0.5f, state.progressFraction, 0.001f)
        assertEquals("00:30", state.formattedPosition)
        assertEquals("01:00", state.formattedDuration)
    }
}
