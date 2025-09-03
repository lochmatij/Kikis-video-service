package com.example.kikisvideoservice.ui

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.kikisvideoservice.data.VideoItem

private fun savePlaybackPosition(context: Context, uri: String, position: Long) {
    val prefs = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
    prefs.edit().putLong(uri, position).apply()
}

private fun getSavedPlaybackPosition(context: Context, uri: String): Long {
    val prefs = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
    return prefs.getLong(uri, 0L)
}

@androidx.media3.common.util.UnstableApi
@Composable
fun VideoPlayerScreen(
    context: Context,
    videoItem: VideoItem,
    onBack: () -> Unit
) {
    val activity = context as Activity

    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoItem.uri))
            prepare()
            playWhenReady = true
        }
    }

    // Link to PlayerView, for controls management
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var controllerVisible by remember { mutableStateOf(false) } // controls visibility

    // Immersive mode
    DisposableEffect(Unit) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Load saved position
        val startPosition = getSavedPlaybackPosition(context, videoItem.uri.toString())
        if (startPosition > 0) {
            exoPlayer.seekTo(startPosition)
        }

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            savePlaybackPosition(context, videoItem.uri.toString(), exoPlayer.currentPosition) // Save position
            exoPlayer.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    savePlaybackPosition(context, videoItem.uri.toString(), exoPlayer.currentPosition) // Save position
                    exoPlayer.playWhenReady = false // pause video
                }
                Lifecycle.Event.ON_RESUME -> {
                    // May leave empty or set playWhenReady = true
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Video
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    findViewById<View>(R.id.exo_settings)?.visibility = View.GONE
                    useController = true
                    setBackgroundColor(android.graphics.Color.BLACK)
                    // Auto hide controls after 3 sec
                    controllerShowTimeoutMs = 3000
                    // Visibility listener, sync with compose Compose
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controllerVisible = (visibility == View.VISIBLE)
                        }
                    )
                }.also { pv -> playerView = pv }
            },
            modifier = Modifier.fillMaxSize(),
            update = { pv ->
                // Show controls on start and run auto hide timer
                pv.showController()
            }
        )

        // Transparent «touch-layer», above PlayerView.
        // Enabled only if controls are hidden to avoid conflicts with default controls
        if (!controllerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                playerView?.showController() // show controls (and back button)
                            }
                        )
                    }
            )
        }

        // Back button — show it when default controls are shown
        if (controllerVisible) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(16.dp)
                        .size(48.dp)
                        // .align(Alignment.TopStart)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onBack() })
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Text(
                    text = videoItem.name,
                    fontSize = 24.sp,
                    color = Color.White
                )
            }

        }
    }
}