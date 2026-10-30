// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.TrackRepository
import com.example.model.AudioFormat
import com.example.model.Track
import com.example.player.AudioPlayerEngine
import com.example.player.PlaybackState
import com.example.player.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main ViewModel orchestrating track segmentation, search, database syncing, and audio playback.
 *
 * ARCHITECTURAL & PERFORMANCE GUIDELINES:
 * 1. Strict MVVM: exposes read-only StateFlows to the UI layer.
 * 2. Vanilla dependency management: instantiated via clean factory without heavy DI overhead.
 * 3. Lifecycle-safe: releases native MediaPlayer immediately when ViewModel is cleared.
 */
class MainViewModel(
    private val repository: TrackRepository,
    context: Context
) : ViewModel() {

    private val playerEngine = AudioPlayerEngine(
        context = context,
        onTrackFinishedCallback = { handleTrackFinished() }
    )

    val playbackState: StateFlow<PlaybackState> = playerEngine.playbackState

    // Internal state signals
    private val _isLoading = MutableStateFlow(false)
    private val _selectedFilter = MutableStateFlow<FilterCategory>(FilterCategory.All)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    private val _selectedFolder = MutableStateFlow<String?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _isFullPlayerVisible = MutableStateFlow(false)

    // Cached current playlist for sequential next/previous playback
    private var currentActivePlaylist: List<Track> = emptyList()

    /**
     * Reactive UI State combining all filter parameters and Room database flow.
     */
    val uiState: StateFlow<MainUiState> = combine(
        repository.allTracks,
        repository.folders,
        _selectedFilter,
        _searchQuery,
        _isSearchActive,
        _sortOption,
        _selectedFolder,
        _isLoading,
        _statusMessage,
        _isFullPlayerVisible
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allTracks = args[0] as List<Track>
        @Suppress("UNCHECKED_CAST")
        val folders = args[1] as List<String>
        val filter = args[2] as FilterCategory
        val query = args[3] as String
        val searchActive = args[4] as Boolean
        val sort = args[5] as SortOption
        val folder = args[6] as String?
        val loading = args[7] as Boolean
        val status = args[8] as String?
        val fullPlayer = args[9] as Boolean

        // Format and favorite count metrics
        var mp3 = 0
        var wav = 0
        var midi = 0
        var favs = 0
        for (t in allTracks) {
            when (t.format) {
                AudioFormat.MP3 -> mp3++
                AudioFormat.WAV -> wav++
                AudioFormat.MIDI -> midi++
            }
            if (t.isFavorite) favs++
        }

        // Apply format and folder filtering
        val filteredByCat = when (filter) {
            is FilterCategory.All -> allTracks
            is FilterCategory.Format -> allTracks.filter { it.format == filter.format }
            is FilterCategory.Favorites -> allTracks.filter { it.isFavorite }
            is FilterCategory.Folders -> {
                if (folder != null) allTracks.filter { it.folderName == folder }
                else allTracks
            }
        }

        // Apply search query
        val searched = if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filteredByCat.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.folderName.lowercase().contains(q)
            }
        } else {
            filteredByCat
        }

        // Apply sorting
        val sorted = when (sort) {
            SortOption.TITLE -> searched.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            SortOption.DURATION -> searched.sortedByDescending { it.durationMs }
            SortOption.SIZE -> searched.sortedByDescending { it.sizeBytes }
        }

        currentActivePlaylist = sorted

        MainUiState(
            isLoading = loading,
            selectedFilter = filter,
            searchQuery = query,
            isSearchActive = searchActive,
            sortOption = sort,
            displayedTracks = sorted,
            allTracksCount = allTracks.size,
            mp3Count = mp3,
            wavCount = wav,
            midiCount = midi,
            favoritesCount = favs,
            folders = folders,
            selectedFolder = folder,
            isFullPlayerVisible = fullPlayer,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(isLoading = true)
    )

    init {
        // Initial silent scan / sample check
        syncInitialData()
    }

    private fun syncInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.scanDeviceAudio()
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Triggers manual media scan across storage.
     */
    fun scanStorage() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Scanning device for MP3, WAV, and MIDI..."
            try {
                val count = repository.scanDeviceAudio()
                _statusMessage.value = if (count > 0) "Indexed $count audio files" else "No new files found. Samples available."
            } catch (e: Exception) {
                _statusMessage.value = "Scan error: ${e.localizedMessage ?: "Unknown"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Injects synthetic .wav and .mid samples into app storage for immediate listening.
     */
    fun generateStarterSamples() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Generating high-efficiency WAV & MIDI test tracks..."
            try {
                val samples = repository.generateStarterSamples()
                _statusMessage.value = "Added ${samples.size} sample files!"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to generate samples: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Imports an external audio file URI picked by user via Storage Access Framework.
     */
    fun importAudioFile(uri: Uri, displayName: String, sizeBytes: Long) {
        viewModelScope.launch {
            try {
                val track = repository.importAudioUri(uri, displayName, sizeBytes)
                _statusMessage.value = "Imported: ${track.title}"
                playTrack(track)
            } catch (e: Exception) {
                _statusMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    fun setFilter(filter: FilterCategory) {
        _selectedFilter.value = filter
        if (filter != FilterCategory.Folders) {
            _selectedFolder.value = null
        }
    }

    fun selectFolder(folder: String?) {
        _selectedFolder.value = folder
        _selectedFilter.value = FilterCategory.Folders
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun setFullPlayerVisible(visible: Boolean) {
        _isFullPlayerVisible.value = visible
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track.id, !track.isFavorite)
        }
    }

    // --- Audio Playback Controls ---

    fun playTrack(track: Track) {
        playerEngine.play(track)
    }

    fun togglePlayPause() {
        val state = playbackState.value
        if (state.isPlaying) {
            playerEngine.pause()
        } else {
            if (state.currentTrack != null) {
                playerEngine.resume()
            } else if (currentActivePlaylist.isNotEmpty()) {
                playerEngine.play(currentActivePlaylist.first())
            }
        }
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun toggleRepeat() {
        playerEngine.toggleRepeatMode()
    }

    fun toggleShuffle() {
        playerEngine.toggleShuffle()
    }

    fun skipNext() {
        val playlist = currentActivePlaylist
        if (playlist.isEmpty()) return

        val state = playbackState.value
        val current = state.currentTrack

        if (state.isShuffleEnabled && playlist.size > 1) {
            val randomTrack = playlist.filter { it.id != current?.id }.randomOrNull() ?: playlist.first()
            playTrack(randomTrack)
            return
        }

        val currentIndex = playlist.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex in 0 until playlist.size - 1) {
            currentIndex + 1
        } else {
            0 // Loop back to start
        }
        playTrack(playlist[nextIndex])
    }

    fun skipPrevious() {
        val playlist = currentActivePlaylist
        if (playlist.isEmpty()) return

        val state = playbackState.value
        // If past 3 seconds, restart current track
        if (state.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        val current = state.currentTrack
        val currentIndex = playlist.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            playlist.size - 1
        }
        playTrack(playlist[prevIndex])
    }

    private fun handleTrackFinished() {
        val state = playbackState.value
        when (state.repeatMode) {
            RepeatMode.REPEAT_ONE -> {
                state.currentTrack?.let { playTrack(it) }
            }
            RepeatMode.REPEAT_ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                val playlist = currentActivePlaylist
                val currentIndex = playlist.indexOfFirst { it.id == state.currentTrack?.id }
                if (currentIndex in 0 until playlist.size - 1) {
                    skipNext()
                } else {
                    playerEngine.stop()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Anti-bloat & Hardware memory guarantee: strictly release audio hardware codecs and coroutines
        playerEngine.release()
    }

    /**
     * Manual Factory to avoid bulky Hilt / Dagger DI frameworks (Vanilla Architecture).
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                val db = AppDatabase.getInstance(context)
                val repo = TrackRepository(context.applicationContext, db)
                return MainViewModel(repo, context.applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
