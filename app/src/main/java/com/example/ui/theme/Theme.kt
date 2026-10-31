// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Three distinct sober, flat (non-gradient) themes for SamPlayer.
 */
enum class AppThemeMode(val title: String, val description: String) {
    DARK("Escuro", "Carvão fosco e ardósia sóbria"),
    LIGHT("Claro", "Branco estúdio e acabamento papel"),
    BLUE("Azul Sóbrio", "Azul marinho nórdico fosco")
}

// 1. Escuro (Sober Dark - Carvão e ardósia matte)
private val SoberDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = Color(0xFF282F3B),
    onSecondaryContainer = DarkTextPrimary,
    tertiary = Color(0xFFCBD5E1),
    onTertiary = Color(0xFF0F172A),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

// 2. Claro (Sober Light - Branco estúdio e contraste tipográfico)
private val SoberLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = LightTextPrimary,
    tertiary = Color(0xFF334155),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

// 3. Azul Sóbrio (Nordic Navy - Marinho fosco sólido, sem gradientes)
private val SoberBlueColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BlueSurfaceVariant,
    onPrimaryContainer = BlueTextPrimary,
    secondary = BlueSecondary,
    onSecondary = BlueOnPrimary,
    secondaryContainer = Color(0xFF243F5E),
    onSecondaryContainer = BlueTextPrimary,
    tertiary = Color(0xFFB0C8E0),
    onTertiary = Color(0xFF0A1522),
    background = BlueBg,
    onBackground = BlueTextPrimary,
    surface = BlueSurface,
    onSurface = BlueTextPrimary,
    surfaceVariant = BlueSurfaceVariant,
    onSurfaceVariant = BlueTextSecondary,
    outline = BlueBorder
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.DARK -> SoberDarkColorScheme
        AppThemeMode.LIGHT -> SoberLightColorScheme
        AppThemeMode.BLUE -> SoberBlueColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
