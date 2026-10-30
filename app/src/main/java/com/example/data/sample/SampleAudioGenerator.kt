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
 * Utility for generating lightweight test audio files (.wav and .mid) directly in app cache.
 *
 * MEMORY & HARDWARE OPTIMIZATION:
 * Generates valid binary audio structures using zero external audio libraries.
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

        // 1. Generate a valid WAV file (44.1kHz, 16-bit Mono, gentle chord arpeggio, ~3 seconds, ~260KB)
        val wavFile = File(outputDir, "Sample_Melody.wav")
        if (!wavFile.exists() || wavFile.length() == 0L) {
            generateArpeggioWav(wavFile, durationSeconds = 3.5)
        }
        if (wavFile.exists()) {
            tracks.add(
                Track(
                    id = 100001L,
                    title = "Ambient Chord Progression",
                    artist = "Prisma Acoustic Synth",
                    durationMs = 3500L,
                    uriString = Uri.fromFile(wavFile).toString(),
                    format = AudioFormat.WAV,
                    folderName = "Starter Samples",
                    sizeBytes = wavFile.length(),
                    dateModified = System.currentTimeMillis()
                )
            )
        }

        // 2. Generate a valid Standard MIDI Format 0 file (~1 KB, plays natively in Android MediaPlayer)
        val midiFile = File(outputDir, "Sample_Cosmic_Theme.mid")
        if (!midiFile.exists() || midiFile.length() == 0L) {
            generateMidiFile(midiFile)
        }
        if (midiFile.exists()) {
            tracks.add(
                Track(
                    id = 100002L,
                    title = "Cosmic Theme Arpeggio",
                    artist = "General MIDI Synthesizer",
                    durationMs = 8000L,
                    uriString = Uri.fromFile(midiFile).toString(),
                    format = AudioFormat.MIDI,
                    folderName = "Starter Samples",
                    sizeBytes = midiFile.length(),
                    dateModified = System.currentTimeMillis()
                )
            )
        }

        tracks
    }

    /**
     * Writes a standard 44.1 kHz 16-bit PCM WAV file with RIFF header.
     */
    private fun generateArpeggioWav(file: File, durationSeconds: Double) {
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
            val frequencies = doubleArrayOf(261.63, 329.63, 392.00, 523.25) // C4, E4, G4, C5

            var sampleIndex = 0
            while (sampleIndex < numSamples) {
                buffer.clear()
                val chunkEnd = minOf(sampleIndex + 1024, numSamples)
                for (i in sampleIndex until chunkEnd) {
                    val t = i.toDouble() / sampleRate
                    val chordIdx = ((t * 2.0).toInt()) % frequencies.size
                    val freq = frequencies[chordIdx]
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

    /**
     * Synthesizes a valid Standard MIDI Format 0 file with notes and tempo.
     * Standard MIDI header: 'MThd' (6 bytes length, format 0, 1 track, 480 division).
     * Track header: 'MTrk' (data bytes length, track events).
     */
    private fun generateMidiFile(file: File) {
        val trackEvents = mutableListOf<Byte>()

        fun writeVarLen(value: Int) {
            var v = value
            val buf = IntArray(4)
            var idx = 0
            buf[idx++] = v and 0x7F
            while ((v shr 7).also { v = it } > 0) {
                buf[idx++] = (v and 0x7F) or 0x80
            }
            while (idx > 0) {
                trackEvents.add(buf[--idx].toByte())
            }
        }

        // Program change: Instrument 0 (Acoustic Grand Piano)
        writeVarLen(0)
        trackEvents.add(0xC0.toByte()) // Program change channel 0
        trackEvents.add(0x00.toByte()) // Acoustic Grand Piano

        // Sequence of musical notes (C, E, G, B, C5, G4, E4, C4)
        val melodyNotes = intArrayOf(60, 64, 67, 71, 72, 67, 64, 60, 62, 65, 69, 72, 60)
        val deltaTicks = 240 // half beat at 480 tpqn

        for (pitch in melodyNotes) {
            // Note On
            writeVarLen(0)
            trackEvents.add(0x90.toByte())
            trackEvents.add(pitch.toByte())
            trackEvents.add(0x64.toByte()) // velocity 100

            // Note Off after deltaTicks
            writeVarLen(deltaTicks)
            trackEvents.add(0x80.toByte())
            trackEvents.add(pitch.toByte())
            trackEvents.add(0x00.toByte())
        }

        // End of Track Meta event (Delta=0, FF 2F 00)
        writeVarLen(0)
        trackEvents.add(0xFF.toByte())
        trackEvents.add(0x2F.toByte())
        trackEvents.add(0x00.toByte())

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(14).order(ByteOrder.BIG_ENDIAN)
            header.put("MThd".toByteArray())
            header.putInt(6) // length
            header.putShort(0.toShort()) // format 0
            header.putShort(1.toShort()) // 1 track
            header.putShort(480.toShort()) // division

            val trackHeader = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            trackHeader.put("MTrk".toByteArray())
            trackHeader.putInt(trackEvents.size)

            fos.write(header.array())
            fos.write(trackHeader.array())
            fos.write(trackEvents.toByteArray())
        }
    }
}
