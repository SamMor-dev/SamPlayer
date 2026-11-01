// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.ui

import com.example.model.AudioFormat
import com.example.model.Track
import com.example.ui.theme.AppThemeMode

/**
 * Filter categories for segmenting audio tracks cleanly by format or custom view.
 */
sealed class FilterCategory {
    object All : FilterCategory()
    data class Format(val format: AudioFormat) : FilterCategory()
    object Favorites : FilterCategory()
    object Folders : FilterCategory()

    val label: String
        get() = when (this) {
            is All -> "All"
            is Format -> format.displayName
            is Favorites -> "Favorites"
            is Folders -> "Folders"
        }
}

/**
 * Sort options for track lists.
 */
enum class SortOption(val label: String) {
    TITLE("Name"),
    DURATION("Duration"),
    SIZE("File Size")
}

/**
 * Complete immutable UI state for the main player screen.
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val selectedFilter: FilterCategory = FilterCategory.All,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortOption: SortOption = SortOption.TITLE,
    val displayedTracks: List<Track> = emptyList(),
    val allTracksCount: Int = 0,
    val formatCounts: Map<AudioFormat, Int> = emptyMap(),
    val favoritesCount: Int = 0,
    val folders: List<String> = emptyList(),
    val selectedFolder: String? = null,
    val isFullPlayerVisible: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val statusMessage: String? = null
) {
    fun countForFormat(format: AudioFormat): Int = formatCounts[format] ?: 0
}
