package com.example.kikisvideoservice

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.media3.common.util.UnstableApi
import com.example.kikisvideoservice.data.VideoItem
import com.example.kikisvideoservice.ui.VideoListScreen
import com.example.kikisvideoservice.ui.VideoPlayerScreen

class MainActivity : ComponentActivity() {
    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var hasPermission by mutableStateOf(false)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> hasPermission = granted }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        setContent {
            MaterialTheme {
                if (hasPermission) {
                    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }

                    if (selectedVideo == null) {
                        VideoListScreen(this) { item ->
                            selectedVideo = item
                        }
                    } else {
                        VideoPlayerScreen(this, selectedVideo!!) {
                            selectedVideo = null
                        }
                    }
                } else {
                    Text("Need access to Movies directory")
                }
            }
        }
    }

}