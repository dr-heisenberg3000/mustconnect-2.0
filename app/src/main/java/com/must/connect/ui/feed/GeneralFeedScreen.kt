package com.must.connect.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.connect.data.model.GeneralFeedPost
import com.must.connect.data.model.UserRole
import com.must.connect.data.repository.AuthRepository
import com.must.connect.ui.theme.BrandNavy
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.filled.AttachFile
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@Composable
fun GeneralFeedScreen(
    paddingValues : PaddingValues,
    viewModel     : FeedViewModel = viewModel(),
    canDelete     : Boolean       = false,   // pass true for SuperAdmin / DeptAdmin
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color(0xFFF7F8FA))
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.posts.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text  = "No announcements yet.",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text  = "Official announcements will appear here.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.posts, key = { it.id }) { post ->
                        FeedPostCard(
                            post      = post,
                            author    = uiState.authors[post.authorId],
                            canDelete = canDelete,
                            onDelete  = { viewModel.deletePost(post.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeedPostCard(
    post      : GeneralFeedPost,
    author    : com.must.connect.data.model.UserProfile?,
    canDelete : Boolean = false,
    onDelete  : () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post") },
            text  = { Text("Are you sure you want to delete '${post.title}'? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            if (post.pinned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint               = BrandNavy,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text       = "PINNED ANNOUNCEMENT",
                        color      = BrandNavy,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Author Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(BrandNavy.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (author?.avatarUrl != null) {
                        AsyncImage(
                            model = author.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = BrandNavy, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = author?.fullName ?: "MUST Admin",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    val formattedDate = try {
                        val instant   = Instant.parse(post.createdAt)
                        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy · HH:mm")
                            .withZone(ZoneId.systemDefault())
                        formatter.format(instant)
                    } catch (e: Exception) {
                        "Recently"
                    }
                    Text(
                        text     = formattedDate,
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text       = post.title,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.Black,
                    modifier   = Modifier.weight(1f)
                )
                if (canDelete) {
                    IconButton(
                        onClick  = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Delete,
                            contentDescription = "Delete Post",
                            tint               = Color(0xFFEF4444),
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text       = post.body,
                fontSize   = 14.sp,
                color      = Color.DarkGray,
                lineHeight = 20.sp
            )

            if (post.attachmentUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (post.attachmentType?.startsWith("image/") == true) {
                    AsyncImage(
                        model = post.attachmentUrl,
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val uriHandler = LocalUriHandler.current
                    OutlinedButton(
                        onClick = { uriHandler.openUri(post.attachmentUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attachment")
                        Spacer(modifier = Modifier.width(8.dp))
                        val isVideo = post.attachmentType?.startsWith("video/") == true
                        Text(if (isVideo) "Watch Video" else (post.attachmentType ?: "View Attachment"))
                    }
                }
            }
        }
    }
}
