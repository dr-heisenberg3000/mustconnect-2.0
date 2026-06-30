package com.must.connect.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.connect.data.model.ClassGroup
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.model.TeacherProfile
import com.must.connect.ui.feed.GeneralFeedScreen
import com.must.connect.ui.theme.AccentBlue
import com.must.connect.ui.theme.BrandNavy
import com.must.connect.ui.profile.EditProfileDialog
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeptAdminDashboardScreen(
    onSignOut         : () -> Unit = {},
    onAddUser         : () -> Unit = {},
    onAddAnnouncement : () -> Unit = {},
    viewModel         : DeptAdminViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    // Password change dialog
    if (uiState.showPasswordDialog) {
        ChangePasswordDialog(
            newPassword       = uiState.newPassword,
            confirmPassword   = uiState.confirmPassword,
            isLoading         = uiState.passwordChangeLoading,
            errorMessage      = uiState.passwordChangeFeedback,
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
            role = com.must.connect.data.model.UserRole.DEPT_ADMIN,
            onDismiss = { showEditProfileDialog = false }
        )
    }

    // Create class dialog
    if (uiState.showCreateClassDialog) {
        CreateClassDialog(
            className  = uiState.newClassName,
            subject    = uiState.newSubject,
            section    = uiState.newSection,
            semester   = uiState.newSemester,
            isLoading  = uiState.createClassLoading,
            onClassNameChange = viewModel::onNewClassNameChange,
            onSubjectChange   = viewModel::onNewSubjectChange,
            onSectionChange   = viewModel::onNewSectionChange,
            onSemesterChange  = viewModel::onNewSemesterChange,
            onSubmit  = viewModel::submitCreateClass,
            onDismiss = viewModel::dismissCreateClassDialog,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dept. of Computer Science & IT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = BrandNavy
                        )
                        Text(
                            text  = uiState.profile?.fullName ?: "Department Admin",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
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
                val navItems = listOf(
                    "Dashboard" to Icons.Default.Dashboard,
                    "Feed"      to Icons.Default.RssFeed,
                    "Users"     to Icons.Default.People,
                    "Classes"   to Icons.Default.Class,
                    "Profile"   to Icons.Default.AccountCircle,
                )
                navItems.forEachIndexed { index, (label, icon) ->
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
            0 -> DeptAdminDashboardContent(
                paddingValues     = paddingValues,
                uiState           = uiState,
                onAddUser         = onAddUser,
                onAddAnnouncement = onAddAnnouncement,
                onCreateClass     = viewModel::showCreateClassDialog,
                onManageStudents  = { selectedTab = 2 }
            )
            1 -> GeneralFeedScreen(
                paddingValues = paddingValues,
                canDelete     = true,
            )
            2 -> DeptAdminUsersTab(
                paddingValues = paddingValues,
                students      = uiState.students,
                teachers      = uiState.teachers,
                onAddUser     = onAddUser,
                isLoading     = uiState.isLoading,
                onResetPassword = viewModel::resetUserPassword,
                onDeleteUser  = viewModel::deleteUser,
            )
            3 -> DeptAdminClassesTab(
                paddingValues = paddingValues,
                uiState       = uiState,
                onCreateClass = viewModel::showCreateClassDialog,
                onAssignTeacher = viewModel::assignTeacherToClass,
                onEnrollStudents = viewModel::enrollStudentsToClass,
            )
            4 -> DeptAdminProfileTab(
                paddingValues      = paddingValues,
                profile            = uiState.profile,
                onChangePassword   = viewModel::showPasswordDialog,
                onSignOut          = onSignOut,
                onEditProfile      = { showEditProfileDialog = true }
            )
        }
    }
}

// ── Dashboard tab ─────────────────────────────────────────────────────────────

@Composable
private fun DeptAdminDashboardContent(
    paddingValues     : PaddingValues,
    uiState           : DeptAdminUiState,
    onAddUser         : () -> Unit,
    onAddAnnouncement : () -> Unit,
    onCreateClass     : () -> Unit,
    onManageStudents  : () -> Unit,
) {
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
                    text = "Welcome, ${uiState.profile?.fullName ?: "Admin"}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Manage your department's academic operations from here.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard2(
                label    = "Students",
                value    = "${uiState.students.size}",
                icon     = Icons.Default.Person,
                modifier = Modifier.weight(1f)
            )
            StatCard2(
                label    = "Teachers",
                value    = "${uiState.teachers.size}",
                icon     = Icons.Default.School,
                modifier = Modifier.weight(1f)
            )
            StatCard2(
                label    = "Classes",
                value    = "${uiState.classes.size}",
                icon     = Icons.Default.Class,
                modifier = Modifier.weight(1f)
            )
        }

        Text("Quick Actions", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)

        // Quick Action Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeptQuickActionCard(
                icon = Icons.Default.PersonAdd,
                title = "Add User",
                modifier = Modifier.weight(1f),
                onClick = onAddUser
            )
            DeptQuickActionCard(
                icon = Icons.Default.Campaign,
                title = "Announcement",
                modifier = Modifier.weight(1f),
                onClick = onAddAnnouncement
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeptQuickActionCard(
                icon = Icons.Default.Class,
                title = "Create Class",
                modifier = Modifier.weight(1f),
                onClick = onCreateClass
            )
            DeptQuickActionCard(
                icon = Icons.Default.Groups,
                title = "Manage Students",
                modifier = Modifier.weight(1f),
                onClick = onManageStudents
            )
        }
    }
}

@Composable
private fun StatCard2(
    label    : String,
    value    : String,
    icon     : ImageVector,
    modifier : Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        border   = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandNavy.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun DeptQuickActionCard(
    icon     : ImageVector,
    title    : String,
    modifier : Modifier = Modifier,
    onClick  : () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        border   = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier            = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = BrandNavy, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }
    }
}

// ── Users tab ─────────────────────────────────────────────────────────────────

@Composable
private fun DeptAdminUsersTab(
    paddingValues: PaddingValues,
    students     : List<StudentProfile>,
    teachers     : List<TeacherProfile>,
    onAddUser    : () -> Unit,
    isLoading    : Boolean,
    onResetPassword: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor   = Color.White,
            contentColor     = BrandNavy
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick  = { selectedTabIndex = 0 },
                text     = { Text("Students", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick  = { selectedTabIndex = 1 },
                text     = { Text("Teachers", fontWeight = FontWeight.SemiBold) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when (selectedTabIndex) {
                    0 -> StudentListContent(students, onResetPassword, onDeleteUser)
                    1 -> TeacherListContent(teachers, onResetPassword, onDeleteUser)
                }
            }
        }

        // FAB-like add button at bottom
        Button(
            onClick  = onAddUser,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            shape    = RoundedCornerShape(100.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = BrandNavy)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New User", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudentListContent(students: List<StudentProfile>, onResetPassword: (String) -> Unit, onDeleteUser: (String) -> Unit) {
    if (students.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No students yet.", color = Color.Gray)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(students, key = { it.id }) { student ->
            UserListCard(
                name       = student.fullName,
                identifier = "Roll: ${student.rollNumber}",
                badge      = student.section,
                icon       = Icons.Default.Person,
                onResetPassword = { onResetPassword(student.id) },
                onDeleteUser = { onDeleteUser(student.userId) }
            )
        }
    }
}

@Composable
private fun TeacherListContent(teachers: List<TeacherProfile>, onResetPassword: (String) -> Unit, onDeleteUser: (String) -> Unit) {
    if (teachers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No teachers yet.", color = Color.Gray)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(teachers, key = { it.id }) { teacher ->
            UserListCard(
                name       = teacher.fullName,
                identifier = "@${teacher.username}",
                badge      = teacher.designation.ifBlank { "Faculty" },
                icon       = Icons.Default.School,
                onResetPassword = { onResetPassword(teacher.id) },
                onDeleteUser = { onDeleteUser(teacher.userId) }
            )
        }
    }
}

@Composable
private fun UserListCard(
    name       : String,
    identifier : String,
    badge      : String,
    icon       : ImageVector,
    onResetPassword: (() -> Unit)? = null,
    onDeleteUser: (() -> Unit)? = null,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showResetDialog && onResetPassword != null) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Password") },
            text = { Text("Are you sure you want to reset the password for $name to 'Password@123'?") },
            confirmButton = {
                TextButton(onClick = {
                    onResetPassword()
                    showResetDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog && onDeleteUser != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete $name? This action cannot be undone and will remove them from all classes.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteUser()
                    showDeleteDialog = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        border   = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandNavy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
                    Text(identifier, fontSize = 12.sp, color = Color.Gray)
                }
                if (badge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = BrandNavy.copy(alpha = 0.08f)
                    ) {
                        Text(
                            badge,
                            fontSize = 11.sp,
                            color    = BrandNavy,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (onResetPassword != null || onDeleteUser != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (onResetPassword != null) {
                        TextButton(onClick = { showResetDialog = true }) {
                            Text("Reset Password", color = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                    if (onDeleteUser != null) {
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("Delete", color = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

// ── Classes tab ───────────────────────────────────────────────────────────────

@Composable
private fun DeptAdminClassesTab(
    paddingValues: PaddingValues,
    uiState: DeptAdminUiState,
    onCreateClass: () -> Unit,
    onAssignTeacher: (String, String) -> Unit,
    onEnrollStudents: (String, List<String>) -> Unit,
) {
    var classToAssignTeacher by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ClassGroup?>(null) }
    var classToEnrollStudents by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ClassGroup?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.classes.isEmpty()) {
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Class,
                        contentDescription = null,
                        tint     = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No classes created yet.", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.classes, key = { it.id }) { group ->
                        val teacher = uiState.teachers.find { it.userId == group.teacherId }
                        val enrolledStudentIds = uiState.classMemberships[group.id] ?: emptyList()
                        val enrolledStudents = uiState.students.filter { enrolledStudentIds.contains(it.userId) }

                        ClassGroupCard(
                            group = group,
                            teacher = teacher,
                            enrolledStudents = enrolledStudents,
                            onAssignTeacherClick = { classToAssignTeacher = group },
                            onEnrollStudentsClick = { classToEnrollStudents = group }
                        )
                    }
                }
            }
        }

        Button(
            onClick  = onCreateClass,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            shape    = RoundedCornerShape(100.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = BrandNavy)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Class", fontWeight = FontWeight.Bold)
        }
    }

    if (classToAssignTeacher != null) {
        var selectedTeacherId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { classToAssignTeacher = null },
            title = { Text("Assign Teacher") },
            text = {
                Column {
                    Text("Select a teacher for ${classToAssignTeacher!!.name}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.teachers.forEach { teacher ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedTeacherId = teacher.userId }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTeacherId == teacher.userId, onClick = { selectedTeacherId = teacher.userId })
                            Text(teacher.fullName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTeacherId?.let { tid ->
                            onAssignTeacher(classToAssignTeacher!!.id, tid)
                            classToAssignTeacher = null
                        }
                    },
                    enabled = selectedTeacherId != null
                ) { Text("Assign") }
            },
            dismissButton = { TextButton(onClick = { classToAssignTeacher = null }) { Text("Cancel") } }
        )
    }

    if (classToEnrollStudents != null) {
        val selectedStudentIds = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateListOf<String>() }
        AlertDialog(
            onDismissRequest = { classToEnrollStudents = null },
            title = { Text("Enroll Students") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    item { Text("Select students for ${classToEnrollStudents!!.name}:") }
                    items(uiState.students) { student ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (selectedStudentIds.contains(student.userId)) selectedStudentIds.remove(student.userId) else selectedStudentIds.add(student.userId)
                            }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedStudentIds.contains(student.userId),
                                onCheckedChange = { chk ->
                                    if (chk) selectedStudentIds.add(student.userId) else selectedStudentIds.remove(student.userId)
                                }
                            )
                            Text(student.fullName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEnrollStudents(classToEnrollStudents!!.id, selectedStudentIds)
                        classToEnrollStudents = null
                    },
                    enabled = selectedStudentIds.isNotEmpty()
                ) { Text("Enroll") }
            },
            dismissButton = { TextButton(onClick = { classToEnrollStudents = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ClassGroupCard(
    group: ClassGroup,
    teacher: TeacherProfile?,
    enrolledStudents: List<StudentProfile>,
    onAssignTeacherClick: () -> Unit,
    onEnrollStudentsClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        border   = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandNavy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Class, contentDescription = null, tint = BrandNavy)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
                    Text(group.subject, fontSize = 13.sp, color = Color.Gray)
                    if (group.section.isNotBlank()) {
                        Text(
                            "Section ${group.section} · Semester ${group.semester}",
                            fontSize = 12.sp,
                            color    = Color.Gray
                        )
                    }
                }
                if (group.isActive) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.1f)
                    ) {
                        Text(
                            "Active",
                            fontSize = 11.sp,
                            color    = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onAssignTeacherClick) { Text("Assign Teacher", color = AccentBlue) }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onEnrollStudentsClick) { Text("Enroll Students", color = AccentBlue) }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide Members" else "Show Members", color = BrandNavy)
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Assigned Teacher", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrandNavy)
                if (teacher != null) {
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(teacher.fullName, fontSize = 14.sp)
                    }
                } else {
                    Text("No teacher assigned.", fontSize = 14.sp, color = Color.Gray)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text("Enrolled Students (${enrolledStudents.size})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrandNavy)
                if (enrolledStudents.isNotEmpty()) {
                    enrolledStudents.forEach { student ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(student.fullName, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(student.rollNumber, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    Text("No students enrolled.", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ── Profile tab ───────────────────────────────────────────────────────────────

@Composable
private fun DeptAdminProfileTab(
    paddingValues    : PaddingValues,
    profile          : com.must.connect.data.model.DeptAdminProfile?,
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
        // Avatar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BrandNavy)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier         = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile?.avatarUrl != null) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text       = profile?.fullName ?: "Department Admin",
                    color      = Color.White,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text     = "@${profile?.username ?: ""}",
                    color    = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        "Department Admin",
                        color    = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Info card
        Card(
            colors   = CardDefaults.cardColors(containerColor = Color.White),
            shape    = RoundedCornerShape(16.dp),
            border   = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Account Details", fontWeight = FontWeight.SemiBold, color = BrandNavy, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))
                ProfileInfoRow("Full Name", profile?.fullName ?: "-")
                ProfileInfoRow("Username", "@${profile?.username ?: "-"}")
                ProfileInfoRow("System Email", profile?.email ?: "-")
                ProfileInfoRow("Status", if (profile?.isActive == true) "Active" else "Inactive")
            }
        }

        // Actions
        OutlinedButton(
            onClick  = onEditProfile,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(100.dp),
            border   = BorderStroke(1.5.dp, BrandNavy)
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BrandNavy)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile Picture", color = BrandNavy, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick  = onChangePassword,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(100.dp),
            border   = BorderStroke(1.5.dp, BrandNavy)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = BrandNavy)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Change Password", color = BrandNavy, fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick  = onSignOut,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(100.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Black)
    }
    Divider(color = Color(0xFFE5E7EB))
}

// ── Shared dialogs ────────────────────────────────────────────────────────────

@Composable
fun ChangePasswordDialog(
    newPassword             : String,
    confirmPassword         : String,
    isLoading               : Boolean,
    errorMessage            : String?,
    onNewPasswordChange     : (String) -> Unit,
    onConfirmPasswordChange : (String) -> Unit,
    onSubmit  : () -> Unit,
    onDismiss : () -> Unit,
) {
    var showNew     by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                OutlinedTextField(
                    value         = newPassword,
                    onValueChange = onNewPasswordChange,
                    label         = { Text("New Password") },
                    singleLine    = true,
                    enabled       = !isLoading,
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon  = {
                        TextButton(onClick = { showNew = !showNew }) {
                            Text(if (showNew) "Hide" else "Show", fontSize = 12.sp)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label         = { Text("Confirm Password") },
                    singleLine    = true,
                    enabled       = !isLoading,
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon  = {
                        TextButton(onClick = { showConfirm = !showConfirm }) {
                            Text(if (showConfirm) "Hide" else "Show", fontSize = 12.sp)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = onSubmit,
                enabled  = !isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateClassDialog(
    className  : String,
    subject    : String,
    section    : String,
    semester   : String,
    isLoading  : Boolean,
    onClassNameChange : (String) -> Unit,
    onSubjectChange   : (String) -> Unit,
    onSectionChange   : (String) -> Unit,
    onSemesterChange  : (String) -> Unit,
    onSubmit   : () -> Unit,
    onDismiss  : () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Class", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = className,
                    onValueChange = onClassNameChange,
                    label         = { Text("Class Name") },
                    placeholder   = { Text("e.g., BSCS-7A") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = subject,
                    onValueChange = onSubjectChange,
                    label         = { Text("Subject") },
                    placeholder   = { Text("e.g., Data Structures") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value         = section,
                        onValueChange = onSectionChange,
                        label         = { Text("Section") },
                        placeholder   = { Text("A") },
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp),
                        modifier      = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value         = semester,
                        onValueChange = onSemesterChange,
                        label         = { Text("Semester") },
                        placeholder   = { Text("7") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape         = RoundedCornerShape(12.dp),
                        modifier      = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onSubmit,
                enabled  = !isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                else Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
