package com.twiapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.twiapp.R
import com.twiapp.data.DownloadRecord
import com.twiapp.services.DownloadTracker
import com.twiapp.ui.theme.PlatformColors
import com.twiapp.ui.theme.SuccessColor
import com.twiapp.ui.theme.ErrorColor
import com.twiapp.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel()
) {
    val records by viewModel.records.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeDownloads by DownloadTracker.activeDownloads.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    // Refresh history when active downloads clear (download finished)
    LaunchedEffect(activeDownloads.size) {
        if (activeDownloads.isEmpty()) viewModel.loadHistory()
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Delete all download history? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) { Text("Clear All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (records.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        contentDescription = "Clear all",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        val hasContent = activeDownloads.isNotEmpty() || records.isNotEmpty()

        if (isLoading && activeDownloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!hasContent) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize().padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No downloads yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your download history will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active downloads section
                if (activeDownloads.isNotEmpty()) {
                    item {
                        Text(
                            "DOWNLOADING",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    items(activeDownloads.values.toList(), key = { "active_${it.id}" }) { download ->
                        ActiveDownloadItem(download = download)
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // Completed downloads section
                if (records.isNotEmpty()) {
                    if (activeDownloads.isNotEmpty()) {
                        item {
                            Text(
                                "COMPLETED",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    items(records, key = { it.id }) { record ->
                        CompletedDownloadItem(
                            record = record,
                            onDelete = { viewModel.deleteRecord(record.id) },
                            onClick = {
                                if (record.contentUri.isNotEmpty() && record.success) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(record.contentUri), "video/*")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(Uri.parse(record.contentUri), "image/*")
                                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Active download card with live progress ─────────────

@Composable
private fun ActiveDownloadItem(download: DownloadTracker.ActiveDownload) {
    val platformColor = getPlatformColor(download.platform)
    val platformIcon = getPlatformIcon(download.platform)

    val statusText = when (download.status) {
        DownloadTracker.Status.FETCHING -> "Fetching info..."
        DownloadTracker.Status.DOWNLOADING -> "${download.progress.toInt()}%"
        DownloadTracker.Status.SAVING -> "Saving..."
        else -> ""
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Platform icon
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = platformColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = platformIcon),
                            contentDescription = download.platform,
                            tint = platformColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.url,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${download.platform} · $statusText",
                        style = MaterialTheme.typography.labelSmall,
                        color = platformColor
                    )
                }

                if (download.eta.isNotEmpty()) {
                    Text(
                        text = download.eta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            if (download.status == DownloadTracker.Status.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { download.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = platformColor,
                    trackColor = platformColor.copy(alpha = 0.1f),
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = platformColor,
                    trackColor = platformColor.copy(alpha = 0.1f),
                )
            }
        }
    }
}

// ─── Completed download item ─────────────

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val idx = digitGroups.coerceAtMost(units.size - 1)
    return String.format("%.1f %s", bytes / Math.pow(1024.0, idx.toDouble()), units[idx])
}

@Composable
private fun CompletedDownloadItem(
    record: DownloadRecord,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val platformColor = getPlatformColor(record.platform)
    val platformIcon = getPlatformIcon(record.platform)
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = record.success && record.contentUri.isNotEmpty()) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail or fallback icon
            if (record.success && record.contentUri.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(record.contentUri))
                        .crossfade(true)
                        .size(224)
                        .build(),
                    contentDescription = "Thumbnail",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = platformColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = platformIcon),
                            contentDescription = record.platform,
                            tint = platformColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Info — takes all remaining space
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title.ifEmpty { record.url },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Row 1: platform + date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = platformIcon),
                        contentDescription = null,
                        tint = platformColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = record.platform,
                        style = MaterialTheme.typography.labelSmall,
                        color = platformColor
                    )
                    Text("·", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Text(
                        text = dateFormat.format(Date(record.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                // Row 2: file size (on its own line so it doesn't cramp)
                if (record.fileSize > 0) {
                    Text(
                        text = formatFileSize(record.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Status icon only (compact)
            if (record.success) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Success",
                    tint = SuccessColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Rounded.Error,
                    contentDescription = "Failed",
                    tint = ErrorColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── Helpers ─────────────

@Composable
private fun getPlatformColor(platform: String): androidx.compose.ui.graphics.Color {
    return when (platform.lowercase()) {
        "tiktok" -> PlatformColors.TikTok
        "instagram" -> PlatformColors.Instagram
        "youtube" -> PlatformColors.YouTube
        "twitter", "twitter / x" -> PlatformColors.Twitter
        "facebook" -> PlatformColors.Facebook
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getPlatformIcon(platform: String): Int {
    return when (platform.lowercase()) {
        "tiktok" -> R.drawable.ic_tiktok
        "instagram" -> R.drawable.ic_instagram
        "youtube" -> R.drawable.ic_youtube
        "twitter", "twitter / x" -> R.drawable.ic_twitter_x
        "facebook" -> R.drawable.ic_facebook
        else -> R.drawable.ic_youtube
    }
}
