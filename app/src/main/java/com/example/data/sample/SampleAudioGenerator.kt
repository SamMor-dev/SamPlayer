// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.data.sample

import android.content.Context
import android.net.Uri
import com.example.model.AudioFormat
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

/**
 * Utility for generating lightweight demo audio files directly in app cache.
 *
 * MEMORY & HARDWARE OPTIMIZATION:
 * Generates valid PCM audio structures using zero external audio libraries.
 * Writes straight to disk via small buffered byte arrays without holding sample buffers in RAM.
 */
object SampleAudioGenerator {

    /**
     * Generates a starter pack of lightweight sample files for instant testing.
     * Runs strictly on Dispatchers.IO.
     */
    suspend fun generateStarterSamples(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val outputDir = File(context.filesDir, "samples").apply { if (!exists()) mkdirs() }

        // 1. Ambient Chord Progression (WAV format, 3.5s)
        val wavMelody = File(outputDir, "Sample_Melody.wav")
        if (!wavMelody.exists() || wavMelody.length() == 0L) {
            generateArpeggioWav(wavMelody, durationSeconds = 3.5, baseFreqs = doubleArrayOf(261.63, 329.63, 392.00, 523.25))
        }
        if (wavMelody.exists()) {
            tracks.add(
                Track(
                    id = 100001L,
                    title = "Ambient Chord Progression",
                    artist = "Prisma Acoustic Synth",
                    durationMs = 3500L,
                    uriString = Uri.fromFile(wavMelody).toString(),
                    format = AudioFormat.WAV,
                    folderName = "Starter Samples",
                    sizeBytes = wavMelody.length(),
                    dateModified = System.currentTimeMillis()
                )
            )
        }

        // 2. Cosmic Bassline Arpeggio (WAV format, 4.0s)
        val wavBass = File(outputDir, "Sample_Cosmic_Bass.wav")
        if (!wavBass.exists() || wavBass.length() == 0L) {
            generateArpeggioWav(wavBass, durationSeconds = 4.0, baseFreqs = doubleArrayOf(130.81, 164.81, 196.00, 261.63))
        }
        if (wavBass.exists()) {
            tracks.add(
                Track(
                    id = 100002L,
                    title = "Cosmic Bass Arpeggio",
                    artist = "Starlight Audio Synth",
                    durationMs = 4000L,
                    uriString = Uri.fromFile(wavBass).toString(),
                    format = AudioFormat.WAV,
                    folderName = "Starter Samples",
                    sizeBytes = wavBass.length(),
                    dateModified = System.currentTimeMillis()
                )
            )
        }

        tracks
    }

    /**
     * Writes a standard 44.1 kHz 16-bit PCM WAV file with RIFF header.
     */
    private fun generateArpeggioWav(file: File, durationSeconds: Double, baseFreqs: DoubleArray) {
        val sampleRate = 44100
        val numSamples = (durationSeconds * sampleRate).toInt()
        val dataSize = numSamples * 2 // 16-bit mono = 2 bytes per sample
        val totalFileSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            // RIFF header
            header.put("RIFF".toByteArray())
            header.putInt(totalFileSize)
            header.put("WAVE".toByteArray())
            // fmt chunk
            header.put("fmt ".toByteArray())
            header.putInt(16) // chunk size
            header.putShort(1.toShort()) // PCM format
            header.putShort(1.toShort()) // 1 channel (mono)
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2) // byte rate
            header.putShort(2.toShort()) // block align
            header.putShort(16.toShort()) // bits per sample
            // data chunk
            header.put("data".toByteArray())
            header.putInt(dataSize)

            fos.write(header.array())

            // Stream samples in small chunks of 1024 samples to preserve RAM
            val buffer = ByteBuffer.allocate(1024 * 2).order(ByteOrder.LITTLE_ENDIAN)

            var sampleIndex = 0
            while (sampleIndex < numSamples) {
                buffer.clear()
                val chunkEnd = minOf(sampleIndex + 1024, numSamples)
                for (i in sampleIndex until chunkEnd) {
                    val t = i.toDouble() / sampleRate
                    val chordIdx = ((t * 2.5).toInt()) % baseFreqs.size
                    val freq = baseFreqs[chordIdx]
                    // Envelope attack/decay
                    val envelope = (1.0 - (t / durationSeconds)).coerceIn(0.0, 1.0)
                    val sampleVal = (sin(2.0 * Math.PI * freq * t) * envelope * 24000).toInt()
                    buffer.putShort(sampleVal.toShort())
                }
                fos.write(buffer.array(), 0, buffer.position())
                sampleIndex = chunkEnd
            }
        }
    }
}
