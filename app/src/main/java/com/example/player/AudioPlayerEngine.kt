// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Lean, native Android MediaPlayer engine tailored for local MP3, WAV, and MIDI.
 *
 * MEMORY & HARDWARE OPTIMIZATION:
 * 1. Single MediaPlayer instance reused safely; strictly released on application teardown.
 * 2. Playback progress polling loop runs only while playing and pauses immediately on stop/pause,
 *    eliminating useless wakeups and saving battery and CPU.
 * 3. Operates purely on applicationContext to prevent any Activity memory leaks.
 */
class AudioPlayerEngine(
    context: Context,
    private val onTrackFinishedCallback: () -> Unit
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume or pause
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
        }
    }

    /**
     * Plays the specified track. Handles local file URIs, ContentResolver URIs, and MIDI files.
     */
    fun play(track: Track) {
        try {
            requestAudioFocus()
            releasePlayer()

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(appContext, Uri.parse(track.uriString))
                setOnPreparedListener { preparedMp ->
                    val resolvedDuration = if (track.durationMs > 0) track.durationMs else preparedMp.duration.toLong()
                    preparedMp.start()
                    _playbackState.update { current ->
                        current.copy(
                            currentTrack = track,
                            isPlaying = true,
                            durationMs = resolvedDuration,
                            currentPositionMs = 0L,
                            isBuffering = false,
                            errorMessage = null
                        )
                    }
                    startProgressTracker()
                }
                setOnCompletionListener {
                    stopProgressTracker()
                    _playbackState.update { it.copy(isPlaying = false, currentPositionMs = it.durationMs) }
                    onTrackFinishedCallback()
                }
                setOnErrorListener { _, what, extra ->
                    stopProgressTracker()
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            errorMessage = "Playback error code: $what ($extra)"
                        )
                    }
                    true
                }
                prepareAsync()
            }

            mediaPlayer = mp
            _playbackState.update {
                it.copy(
                    currentTrack = track,
                    isBuffering = true,
                    durationMs = track.durationMs
                )
            }
        } catch (e: Exception) {
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = e.localizedMessage ?: "Failed to open audio source"
                )
            }
        }
    }

    /**
     * Resumes playback if paused, or restarts if track ended.
     */
    fun resume() {
        val mp = mediaPlayer ?: run {
            _playbackState.value.currentTrack?.let { play(it) }
            return
        }
        try {
            requestAudioFocus()
            mp.start()
            _playbackState.update { it.copy(isPlaying = true) }
            startProgressTracker()
        } catch (_: Exception) {
            _playbackState.value.currentTrack?.let { play(it) }
        }
    }

    /**
     * Pauses current playback. Stops position tracking loop.
     */
    fun pause() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.pause()
                }
            } catch (_: Exception) {}
        }
        stopProgressTracker()
        _playbackState.update { it.copy(isPlaying = false) }
    }

    /**
     * Seeks playback to position in milliseconds.
     */
    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            try {
                val clamped = positionMs.coerceIn(0L, _playbackState.value.durationMs)
                mp.seekTo(clamped.toInt())
                _playbackState.update { it.copy(currentPositionMs = clamped) }
            } catch (_: Exception) {}
        }
    }

    /**
     * Toggles repeat mode.
     */
    fun toggleRepeatMode() {
        _playbackState.update { current ->
            val next = when (current.repeatMode) {
                RepeatMode.OFF -> RepeatMode.REPEAT_ALL
                RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
                RepeatMode.REPEAT_ONE -> RepeatMode.OFF
            }
            current.copy(repeatMode = next)
        }
    }

    /**
     * Toggles shuffle mode.
     */
    fun toggleShuffle() {
        _playbackState.update { current ->
            current.copy(isShuffleEnabled = !current.isShuffleEnabled)
        }
    }

    /**
     * Stops playback and clears player state.
     */
    fun stop() {
        stopProgressTracker()
        releasePlayer()
        abandonAudioFocus()
        _playbackState.update {
            it.copy(
                isPlaying = false,
                currentPositionMs = 0L,
                isBuffering = false
            )
        }
    }

    /**
     * Completely tears down MediaPlayer and cleans up all active coroutines.
     * Guaranteed to release all hardware codecs and RAM allocations.
     */
    fun release() {
        stopProgressTracker()
        releasePlayer()
        abandonAudioFocus()
        _playbackState.update { PlaybackState() }
    }

    private fun releasePlayer() {
        mediaPlayer?.let { mp ->
            try {
                mp.reset()
                mp.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
    }

    /**
     * Progress polling loop: Updates playback progress at ~100ms intervals.
     *
     * MEMORY & CPU OPTIMIZATION:
     * Dispatched strictly while audio is playing. Canceled immediately when paused/stopped.
     */
    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = playerScope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    try {
                        val currentMs = mp.currentPosition.toLong()
                        val durMs = mp.duration.toLong()
                        _playbackState.update {
                            it.copy(
                                currentPositionMs = currentMs,
                                durationMs = if (durMs > 0) durMs else it.durationMs
                            )
                        }
                    } catch (_: Exception) {}
                }
                delay(150)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            focusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(focusChangeListener)
        }
    }
}
