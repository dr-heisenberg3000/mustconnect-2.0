package com.must.connect.ui.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.connect.data.model.ClassGroup
import com.must.connect.data.model.ClassPost
import com.must.connect.ui.admin.ChangePasswordDialog
import com.must.connect.ui.feed.GeneralFeedScreen
import com.must.connect.ui.profile.EditProfileDialog
import com.must.connect.ui.theme.BrandNavy
import coil.compose.AsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CalendarMonth

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    onSignOut : () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    viewModel : StudentViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var showNotificationsDropdown by remember { mutableStateOf(false) }
    val feedViewModel: com.must.connect.ui.feed.FeedViewModel = viewModel()
    val feedUiState by feedViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearFeedback() }
    }

    if (uiState.showPasswordDialog) {
        ChangePasswordDialog(
            newPassword             = uiState.newPassword,
            confirmPassword         = uiState.confirmPassword,
            isLoading               = uiState.passwordChangeLoading,
            errorMessage            = uiState.passwordChangeFeedback,
            onNewPasswordChange     = viewModel::onNewPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onSubmit  = viewModel::submitPasswordChange,
            onDismiss = viewModel::dismissPasswordDialog,
        )
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }

    if (showEditProfileDialog && uiState.profile != null) {
        EditProfileDialog(
            currentAvatarUrl = uiState.profile?.avatarUrl,
            userId = uiState.profile!!.userId,
            role = com.must.connect.data.model.UserRole.STUDENT,
            onDismiss = { showEditProfileDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToChat,
                containerColor = BrandNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Messages")
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch && (selectedTab == 1 || selectedTab == 2)) {
                        TextField(
                            value = if (selectedTab == 1) feedUiState.searchQuery else uiState.classSearchQuery,
                            onValueChange = { 
                                if (selectedTab == 1) feedViewModel.updateSearchQuery(it)
                                else viewModel.updateClassSearchQuery(it)
                            },
                            placeholder = { Text("Search...", color = Color.Gray) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text("MUST-CONNECT", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = BrandNavy)
                            Text("Student Portal", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    if (selectedTab == 1) {
                        val uriHandler = LocalUriHandler.current
                        IconButton(onClick = {
                            val url = "https://naosswlwqwrllieuxsvi.supabase.co/storage/v1/object/public/timetables/student_timetable.pdf"
                            uriHandler.openUri(url)
                        }) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "View Timetable", tint = BrandNavy)
                        }
                    }
                    if (selectedTab == 1 || selectedTab == 2) {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search", tint = BrandNavy)
                        }
                    }
                    Box {
                        IconButton(onClick = { showNotificationsDropdown = true }) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = BrandNavy
                            )
                            if (uiState.newPostCount > 0 || uiState.unreadDmCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showNotificationsDropdown,
                            onDismissRequest = { showNotificationsDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            if (uiState.unreadDmCount == 0 && uiState.newPostCount == 0) {
                                DropdownMenuItem(
                                    text = { Text("No new notifications", color = Color.Gray) },
                                    onClick = { showNotificationsDropdown = false }
                                )
                            } else {
                                if (uiState.unreadDmCount > 0) {
                                    DropdownMenuItem(
                                        text = { Text("${uiState.unreadDmCount} unread message(s)", color = BrandNavy, fontWeight = FontWeight.SemiBold) },
                                        onClick = { showNotificationsDropdown = false; onNavigateToChat() }
                                    )
                                }
                                if (uiState.newPostCount > 0) {
                                    DropdownMenuItem(
                                        text = { Text("${uiState.newPostCount} new feed posts", color = BrandNavy, fontWeight = FontWeight.SemiBold) },
                                        onClick = { showNotificationsDropdown = false; selectedTab = 1 }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = BrandNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F8FA))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val tabs = listOf(
                    "Home"    to Icons.Default.Home,
                    "Feed"    to Icons.Default.RssFeed,
                    "Classes" to Icons.Default.Class,
                    "Profile" to Icons.Default.AccountCircle,
                )
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        icon     = { Icon(icon, contentDescription = label) },
                        label    = { Text(label, fontSize = 11.sp) },
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index }
                    )
                }
            }
        },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        when (selectedTab) {
            0 -> StudentHomeTab(paddingValues, uiState, onClassClick = { classId -> 
                viewModel.selectClass(classId)
                selectedTab = 2 
            })
            1 -> GeneralFeedScreen(paddingValues = paddingValues, canDelete = false)
            2 -> StudentClassesTab(paddingValues, uiState, viewModel::selectClass, viewModel::updateClassSearchQuery)
            3 -> StudentProfileTab(paddingValues, uiState, viewModel::showPasswordDialog, onSignOut, onEditProfile = { showEditProfileDialog = true })
        }
    }
}

// ── Home tab ──────────────────────────────────────────────────────────────────

@Composable
private fun StudentHomeTab(paddingValues: PaddingValues, uiState: StudentUiState, onClassClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BrandNavy)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Hello, ${uiState.profile?.fullName?.split(" ")?.first() ?: "Student"}!",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Roll: ${uiState.profile?.rollNumber ?: ""} · Semester ${uiState.profile?.semester ?: ""} · Section ${uiState.profile?.section ?: ""}",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp
                )
            }
        }

        // Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StudentStatCard("Enrolled Classes", "${uiState.myClasses.size}", Icons.Default.Class, Modifier.weight(1f))
            StudentStatCard("Recent Posts", "${uiState.classPosts.size}", Icons.Default.Article, Modifier.weight(1f))
        }

        // Enrolled classes
        if (uiState.myClasses.isNotEmpty()) {
            Text("Enrolled Classes", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            uiState.myClasses.forEach { group ->
                StudentClassCard(group, onClick = { onClassClick(group.id) })
            }
        } else if (!uiState.isLoading) {
            Card(
                colors   = CardDefaults.cardColors(containerColor = Color.White),
                shape    = RoundedCornerShape(16.dp),
                border   = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Class, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Not enrolled in any class yet.", color = Color.Gray)
                    Text("Contact your Department Admin.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StudentStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        border   = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier.size(36.dp).clip(CircleShape).background(BrandNavy.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = BrandNavy, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun StudentClassCard(group: ClassGroup, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        border   = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(BrandNavy.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.MenuBook, null, tint = BrandNavy) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(group.subject, fontSize = 13.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

// ── Classes tab ───────────────────────────────────────────────────────────────

@Composable
private fun StudentClassesTab(
    paddingValues : PaddingValues,
    uiState       : StudentUiState,
    onSelectClass : (String?) -> Unit,
    onUpdateSearchQuery: (String) -> Unit
) {
    val myClasses = uiState.myClasses
    if (myClasses.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            Text("Not enrolled in any class.", color = Color.Gray)
        }
        return
    }

    if (uiState.selectedClassId == null) {
        // "My Classes" List View
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Enrolled Classes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(myClasses) { group ->
                    MyClassCard(group) { onSelectClass(group.id) }
                }
            }
        }
    } else {
        // "Class Information" Detail View
        val selectedClass = myClasses.find { it.id == uiState.selectedClassId } ?: return
        
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF7F8FA))) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onSelectClass(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedClass.subject, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(100.dp), color = BrandNavy.copy(alpha = 0.1f)) {
                                Text(selectedClass.name, fontSize = 11.sp, color = BrandNavy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sec ${selectedClass.section}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
            
            // Class posts
            val filteredPosts = if (uiState.classSearchQuery.isBlank()) {
                uiState.classPosts
            } else {
                val query = uiState.classSearchQuery.lowercase()
                uiState.classPosts.filter { post ->
                    val authorName = uiState.postAuthors[post.authorId]?.fullName?.lowercase() ?: ""
                    post.title.lowercase().contains(query) ||
                    post.body.lowercase().contains(query) ||
                    authorName.contains(query) ||
                    (post.attachmentUrl?.lowercase()?.contains(query) == true)
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (filteredPosts.isEmpty()) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Article, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (uiState.classPosts.isEmpty()) "No posts in this class yet." else "No posts found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredPosts, key = { it.id }) { post ->
                            StudentClassPostCard(post, author = uiState.postAuthors[post.authorId])
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyClassCard(group: ClassGroup, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(100.dp), color = BrandNavy.copy(alpha = 0.1f)) {
                    Text(group.name, fontSize = 12.sp, color = BrandNavy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFFF3F4F6)) {
                    Text("Semester ${group.semester}", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(group.subject, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Section ${group.section}", fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun StudentClassPostCard(post: ClassPost, author: com.must.connect.data.model.UserProfile?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandNavy.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!author?.avatarUrl.isNullOrEmpty()) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = author?.fullName ?: "Teacher",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    val date = try {
                        val f = DateTimeFormatter.ofPattern("MMM dd · HH:mm").withZone(ZoneId.systemDefault())
                        f.format(Instant.parse(post.createdAt))
                    } catch (e: Exception) { "Recently" }
                    Text(date, fontSize = 12.sp, color = Color.Gray)
                }
                Surface(shape = RoundedCornerShape(100.dp), color = if (post.postType == "ASSIGNMENT") Color(0xFFFFF7ED) else BrandNavy.copy(alpha = 0.1f)) {
                    Text(post.postType, fontSize = 10.sp, color = if (post.postType == "ASSIGNMENT") Color(0xFFC2410C) else BrandNavy, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Text(post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(post.body, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
            
            if (post.attachmentUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val uriHandler = LocalUriHandler.current
                val context = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri(post.attachmentUrl) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attachment")
                        Spacer(modifier = Modifier.width(8.dp))
                        val displayFileName = try {
                            val uriSegment = android.net.Uri.parse(post.attachmentUrl).lastPathSegment ?: "View File"
                            if (uriSegment.contains("_")) uriSegment.substringAfter("_") else uriSegment
                        } catch (e: Exception) { "View File" }
                        Text(displayFileName, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    
                    IconButton(
                        onClick = {
                            try {
                                val request = DownloadManager.Request(Uri.parse(post.attachmentUrl))
                                val fileName = Uri.parse(post.attachmentUrl).lastPathSegment ?: "downloaded_file"
                                request.setTitle(fileName)
                                request.setDescription("Downloading file from MUST CONNECT")
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.enqueue(request)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                }
            }

            if (post.postType == "ASSIGNMENT") {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* Submit logic */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                ) {
                    Text("Submit Assignment")
                }
            }
        }
    }
}

// ── Profile tab ───────────────────────────────────────────────────────────────

@Composable
private fun StudentProfileTab(
    paddingValues    : PaddingValues,
    uiState          : StudentUiState,
    onChangePassword : () -> Unit,
    onSignOut        : () -> Unit,
    onEditProfile    : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BrandNavy).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) { 
                    if (uiState.profile?.avatarUrl != null) {
                        AsyncImage(
                            model = uiState.profile!!.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(48.dp)) 
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.profile?.fullName ?: "Student", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(uiState.profile?.rollNumber ?: "", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(100.dp), color = Color.White.copy(alpha = 0.2f)) {
                    Text("Student", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
            }
        }

        Card(
            colors   = CardDefaults.cardColors(containerColor = Color.White),
            shape    = RoundedCornerShape(16.dp),
            border   = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Academic Profile", fontWeight = FontWeight.SemiBold, color = BrandNavy, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))
                StudentInfoRow("Full Name",   uiState.profile?.fullName ?: "-")
                StudentInfoRow("Roll Number", uiState.profile?.rollNumber ?: "-")
                StudentInfoRow("Section",     uiState.profile?.section?.ifBlank { "-" } ?: "-")
                StudentInfoRow("Semester",    "${uiState.profile?.semester ?: "-"}")
                StudentInfoRow("Status",      if (uiState.profile?.isActive == true) "Active" else "Inactive")
            }
        }

        OutlinedButton(
            onClick  = onEditProfile,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(100.dp),
            border   = BorderStroke(1.5.dp, BrandNavy)
        ) {
            Icon(Icons.Default.AccountCircle, null, tint = BrandNavy)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile Picture", color = BrandNavy, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick  = onChangePassword,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(100.dp),
            border   = BorderStroke(1.5.dp, BrandNavy)
        ) {
            Icon(Icons.Default.Lock, null, tint = BrandNavy)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Change Password", color = BrandNavy, fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick  = onSignOut,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(100.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
            Icon(Icons.Default.Logout, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudentInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
    HorizontalDivider(color = Color(0xFFE5E7EB))
}
