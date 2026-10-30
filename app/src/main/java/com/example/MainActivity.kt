// Copyright (c) 2026, Sam Moreno. All rights reserved.
// Use of this source code is governed by a BSD 2-Clause license.

package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme

/**
 * Main Activity for Prisma Player.
 *
 * MEMORY LEAK PREVENTION NOTE:
 * - Does not leak Activity context to singletons or audio engines.
 * - Passes only applicationContext to ViewModel factory.
 * - Handled gracefully on configuration change and destruction.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val appCtx = applicationContext

                // Resolve runtime audio permission depending on Android OS version
                val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(appCtx, audioPermission) == PackageManager.PERMISSION_GRANTED
                    )
                }

                // Instantiate ViewModel with vanilla Factory (Zero Hilt/Dagger bloat)
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(appCtx)
                )

                // Permission request launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasPermission = isGranted
                    if (isGranted) {
                        viewModel.scanStorage()
                    }
                }

                LaunchedEffect(Unit) {
                    if (hasPermission) {
                        viewModel.scanStorage()
                    }
                }

                MainScreen(
                    viewModel = viewModel,
                    hasAudioPermission = hasPermission,
                    onRequestPermission = {
                        permissionLauncher.launch(audioPermission)
                    }
                )
            }
        }
    }
}
