package com.must.connect.ui.teacher

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
import com.must.connect.data.model.StudentProfile
import com.must.connect.ui.admin.ChangePasswordDialog
import com.must.connect.ui.feed.GeneralFeedScreen
import com.must.connect.ui.profile.EditProfileDialog
import com.must.connect.ui.theme.BrandNavy
import coil.compose.AsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    onSignOut  : () -> Unit,
    onNavigateToChat: () -> Unit,
    viewModel  : TeacherViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearFeedback() }
    }

    // Password dialog
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

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    viewModel.onAttachmentSelected(uri, cursor.getString(nameIndex))
                }
            }
        } else {
            viewModel.onAttachmentSelected(null, null)
        }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }

    if (showEditProfileDialog && uiState.profile != null) {
        EditProfileDialog(
            currentAvatarUrl = uiState.profile?.avatarUrl,
            userId = uiState.profile!!.userId,
            role = com.must.connect.data.model.UserRole.TEACHER,
            onDismiss = { showEditProfileDialog = false }
        )
    }

    // Create post dialog
    if (uiState.showCreatePostDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCreatePostDialog,
            title = { Text("New Class Post", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.newPostTitle,
                        onValueChange = viewModel::onNewPostTitleChange,
                        label = { Text("Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.newPostBody,
                        onValueChange = viewModel::onNewPostBodyChange,
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
                        shape = RoundedCornerShape(12.dp),
                    )
                    
                    if (uiState.newPostAttachmentUri != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0F4FF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = BrandNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.newPostAttachmentName ?: "Selected File",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                color = BrandNavy
                            )
                            IconButton(onClick = { viewModel.onAttachmentSelected(null, null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { launcher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select File")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = { viewModel.submitCreatePost(context) },
                    enabled  = !uiState.createPostLoading,
                    colors   = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                ) {
                    if (uiState.createPostLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    else Text("Post")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissCreatePostDialog) { Text("Cancel") } }
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
                    Column {
                        Text("MUST-CONNECT", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = BrandNavy)
                        Text("Faculty Portal", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F8FA))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val tabs = listOf(
                    "Overview"  to Icons.Default.Dashboard,
                    "Feed"      to Icons.Default.RssFeed,
                    "Classes"   to Icons.Default.Class,
                    "Profile"   to Icons.Default.AccountCircle,
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
            0 -> TeacherOverviewTab(paddingValues, uiState, onClassClick = { classId ->
                viewModel.selectClass(classId)
                selectedTab = 2
            })
            1 -> GeneralFeedScreen(paddingValues = paddingValues, canDelete = false)
            2 -> TeacherClassesTab(
                paddingValues   = paddingValues,
                uiState         = uiState,
                onSelectClass   = viewModel::selectClass,
                onCreatePost    = viewModel::showCreatePostDialog,
                onDeletePost    = { postId -> viewModel.deletePost(postId) },
            )
            3 -> TeacherProfileTab(
                paddingValues    = paddingValues,
                uiState          = uiState,
                onChangePassword = viewModel::showPasswordDialog,
                onSignOut        = onSignOut,
                onEditProfile    = { showEditProfileDialog = true }
            )
        }
    }
}

// ── Overview tab ──────────────────────────────────────────────────────────────

@Composable
private fun TeacherOverviewTab(paddingValues: PaddingValues, uiState: TeacherUiState, onClassClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BrandNavy)
                .padding(20.dp)
        ) {
            Column {
                Text("Good day, ${uiState.profile?.fullName?.split(" ")?.first() ?: "Teacher"}!", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(uiState.profile?.designation?.ifBlank { "Faculty Member" } ?: "Faculty Member", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TeacherStatCard("My Classes", "${uiState.myClasses.size}", Icons.Default.Class, Modifier.weight(1f))
            TeacherStatCard("Students", "${uiState.classMembers.size}", Icons.Default.People, Modifier.weight(1f))
        }

        if (uiState.myClasses.isNotEmpty()) {
            Text("My Classes", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            uiState.myClasses.forEach { group ->
                TeacherClassSummaryCard(group, onClick = { onClassClick(group.id) })
            }
        } else if (!uiState.isLoading) {
            Card(
                colors   = CardDefaults.cardColors(containerColor = Color.White),
                shape    = RoundedCornerShape(16.dp),
                border   = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Class, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No classes assigned yet.", color = Color.Gray)
                    Text("Contact the Dept Admin to be assigned to a class.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TeacherStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
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
private fun TeacherClassSummaryCard(group: ClassGroup, onClick: () -> Unit = {}) {
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
            ) { Icon(Icons.Default.Class, null, tint = BrandNavy) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(group.subject, fontSize = 13.sp, color = Color.Gray)
                if (group.section.isNotBlank()) {
                    Text("Sec ${group.section} · Sem ${group.semester}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ── Classes tab ───────────────────────────────────────────────────────────────

@Composable
private fun TeacherClassesTab(
    paddingValues : PaddingValues,
    uiState       : TeacherUiState,
    onSelectClass : (String?) -> Unit,
    onCreatePost  : () -> Unit,
    onDeletePost  : (String) -> Unit,
) {
    val myClasses = uiState.myClasses
    if (myClasses.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            Text("No classes assigned.", color = Color.Gray)
        }
        return
    }

    if (uiState.selectedClassId == null) {
        // "My Classes" List View
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Classes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
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
            Box(modifier = Modifier.fillMaxWidth().background(BrandNavy).padding(16.dp)) {
                Column {
                    IconButton(onClick = { onSelectClass(null) }, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(selectedClass.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${selectedClass.subject} · Section ${selectedClass.section}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
            
            // Tabs (Mocked visual for now)
            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 12.dp, horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Feed", fontWeight = FontWeight.Bold, color = BrandNavy)
                Text("Resources", color = Color.Gray)
                Text("Assignments", color = Color.Gray)
            }
            HorizontalDivider(color = Color(0xFFE5E7EB))

            // Posts list
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.classPosts.isEmpty()) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PostAdd, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No posts yet. Create the first one!", color = Color.Gray)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.classPosts, key = { it.id }) { post ->
                            TeacherClassPostCard(post, onDeletePost)
                        }
                    }
                }
            }

            Button(
                onClick  = onCreatePost,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                shape    = RoundedCornerShape(100.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                Icon(Icons.Default.PostAdd, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Post", fontWeight = FontWeight.Bold)
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
private fun TeacherClassPostCard(post: ClassPost, onDelete: (String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post") },
            text  = { Text("Delete '${post.title}'?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete(post.id) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
            val date = try {
                val f = DateTimeFormatter.ofPattern("MMM dd, HH:mm").withZone(ZoneId.systemDefault())
                f.format(Instant.parse(post.createdAt))
            } catch (e: Exception) { "Recently" }
            Text(date, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(post.body, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
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

// ── Profile tab ───────────────────────────────────────────────────────────────

@Composable
private fun TeacherProfileTab(
    paddingValues    : PaddingValues,
    uiState          : TeacherUiState,
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
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(48.dp)) 
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.profile?.fullName ?: "Teacher", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("@${uiState.profile?.username ?: ""}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(100.dp), color = Color.White.copy(alpha = 0.2f)) {
                    Text(
                        uiState.profile?.designation?.ifBlank { "Faculty" } ?: "Faculty",
                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
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
                Text("Account Details", fontWeight = FontWeight.SemiBold, color = BrandNavy, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))
                TeacherInfoRow("Full Name", uiState.profile?.fullName ?: "-")
                TeacherInfoRow("Username",  "@${uiState.profile?.username ?: "-"}")
                TeacherInfoRow("Designation", uiState.profile?.designation?.ifBlank { "-" } ?: "-")
                TeacherInfoRow("Status", if (uiState.profile?.isActive == true) "Active" else "Inactive")
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
private fun TeacherInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
    HorizontalDivider(color = Color(0xFFE5E7EB))
}
