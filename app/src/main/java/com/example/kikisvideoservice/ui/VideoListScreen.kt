@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.kikisvideoservice.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kikisvideoservice.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.kikisvideoservice.data.VideoRepository

@Composable
fun VideoListScreen(context: Context, onVideoClick: (VideoItem) -> Unit) {
    val videos = remember { VideoRepository.getVideos(context) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Local videos") })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(videos) { item ->
                VideoListItem(item, onClick = { onVideoClick(item) })
            }
        }
    }
}

@Composable
fun VideoListItem(item: VideoItem, onClick: () -> Unit) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var duration by remember { mutableStateOf("") }

    LaunchedEffect(item.uri) {
        thumbnail = loadVideoThumbnail(context, item.uri)
        duration = getVideoDuration(context, item.uri)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Preview
        thumbnail?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } ?: Box(
            modifier = Modifier
                .size(80.dp)
                .padding(8.dp)
        )

        Spacer(Modifier.width(16.dp))

        // Text part
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (duration.isNotEmpty()) {
                Text(
                    text = duration,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

suspend fun loadVideoThumbnail(context: Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(1_000_000 * 60) // 1 minute
            retriever.release()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

suspend fun getVideoDuration(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()

            val minutes = (time / 1000) / 60
            val seconds = (time / 1000) % 60
            String.format("%d:%02d", minutes, seconds)
        } catch (e: Exception) {
            ""
        }
    }
}