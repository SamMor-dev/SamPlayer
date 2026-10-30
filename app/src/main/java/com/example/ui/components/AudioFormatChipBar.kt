// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioFormat
import com.example.ui.FilterCategory
import com.example.ui.MainUiState

/**
 * Clean segmented format bar to isolate tracks by their audio container (.mp3, .wav, .mid).
 *
 * MEMORY & RENDERING EFFICIENCY:
 * Rendered using lightweight Compose FilterChips without recreating state models.
 */
@Composable
fun AudioFormatChipBar(
    uiState: MainUiState,
    onFilterSelected: (FilterCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. ALL
        val isAllSelected = uiState.selectedFilter is FilterCategory.All
        FilterChip(
            selected = isAllSelected,
            onClick = { onFilterSelected(FilterCategory.All) },
            label = {
                Text(
                    text = "All (${uiState.allTracksCount})",
                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                )
            },
            modifier = Modifier.testTag("filter_chip_all")
        )

        // 2. MP3
        val isMp3Selected = (uiState.selectedFilter as? FilterCategory.Format)?.format == AudioFormat.MP3
        FilterChip(
            selected = isMp3Selected,
            onClick = { onFilterSelected(FilterCategory.Format(AudioFormat.MP3)) },
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "MP3",
                        color = if (isMp3Selected) MaterialTheme.colorScheme.onPrimaryContainer else AudioFormat.MP3.accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text("(${uiState.mp3Count})")
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AudioFormat.MP3.accentColor.copy(alpha = 0.25f),
                selectedLabelColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.testTag("filter_chip_mp3")
        )

        // 3. WAV
        val isWavSelected = (uiState.selectedFilter as? FilterCategory.Format)?.format == AudioFormat.WAV
        FilterChip(
            selected = isWavSelected,
            onClick = { onFilterSelected(FilterCategory.Format(AudioFormat.WAV)) },
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "WAV",
                        color = if (isWavSelected) MaterialTheme.colorScheme.onPrimaryContainer else AudioFormat.WAV.accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text("(${uiState.wavCount})")
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AudioFormat.WAV.accentColor.copy(alpha = 0.25f),
                selectedLabelColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.testTag("filter_chip_wav")
        )

        // 4. MIDI (.mid)
        val isMidiSelected = (uiState.selectedFilter as? FilterCategory.Format)?.format == AudioFormat.MIDI
        FilterChip(
            selected = isMidiSelected,
            onClick = { onFilterSelected(FilterCategory.Format(AudioFormat.MIDI)) },
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "MIDI (.mid)",
                        color = if (isMidiSelected) MaterialTheme.colorScheme.onPrimaryContainer else AudioFormat.MIDI.accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text("(${uiState.midiCount})")
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AudioFormat.MIDI.accentColor.copy(alpha = 0.25f),
                selectedLabelColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.testTag("filter_chip_midi")
        )

        // 5. FAVORITES
        val isFavSelected = uiState.selectedFilter is FilterCategory.Favorites
        FilterChip(
            selected = isFavSelected,
            onClick = { onFilterSelected(FilterCategory.Favorites) },
            leadingIcon = {
                Icon(
                    imageVector = if (isFavSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavSelected) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = {
                Text(
                    text = "Favorites (${uiState.favoritesCount})",
                    fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Medium
                )
            },
            modifier = Modifier.testTag("filter_chip_favorites")
        )

        // 6. FOLDERS
        val isFoldersSelected = uiState.selectedFilter is FilterCategory.Folders
        FilterChip(
            selected = isFoldersSelected,
            onClick = { onFilterSelected(FilterCategory.Folders) },
            leadingIcon = {
                Icon(
                    imageVector = if (isFoldersSelected) Icons.Filled.Folder else Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = {
                val folderLabel = uiState.selectedFolder ?: "Folders (${uiState.folders.size})"
                Text(
                    text = folderLabel,
                    fontWeight = if (isFoldersSelected) FontWeight.Bold else FontWeight.Medium
                )
            },
            modifier = Modifier.testTag("filter_chip_folders")
        )
    }
}
