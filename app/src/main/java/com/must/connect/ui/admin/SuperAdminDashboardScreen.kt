package com.must.connect.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.connect.data.model.ClassGroup
import com.must.connect.data.model.DeptAdminProfile
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.model.TeacherProfile
import com.must.connect.ui.feed.GeneralFeedScreen
import com.must.connect.ui.profile.EditProfileDialog
import com.must.connect.ui.theme.BrandNavy
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    onSignOut : () -> Unit = {},
    viewModel : SuperAdminViewModel = viewModel(),
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
            role = com.must.connect.data.model.UserRole.SUPER_ADMIN,
            onDismiss = { showEditProfileDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "MUST-CONNECT",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = BrandNavy
                        )
                        Text(
                            "System Command Center",
                            fontSize = 12.sp,
                            color    = Color.Gray
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
                    "Overview" to Icons.Default.Dashboard,
                    "Feed"     to Icons.Default.RssFeed,
                    "Users"    to Icons.Default.ManageAccounts,
                    "Classes"  to Icons.Default.Class,
                    "Profile"  to Icons.Default.AccountCircle,
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
            0 -> SuperAdminOverviewTab(paddingValues, uiState)
            1 -> GeneralFeedScreen(paddingValues = paddingValues, canDelete = true)
            2 -> SuperAdminUsersTab(paddingValues, uiState)
            3 -> SuperAdminClassesTab(paddingValues, uiState)
            4 -> SuperAdminProfileTab(
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
private fun SuperAdminOverviewTab(paddingValues: PaddingValues, uiState: SuperAdminUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BrandNavy)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "System Command Center",
                    color      = Color.White,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Welcome, ${uiState.profile?.fullName ?: "Super Admin"}. Monitor and manage all platform activity.",
                    color    = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SuperStatCard("Students",  "${uiState.students.size}",  Icons.Default.Person,  Modifier.weight(1f))
            SuperStatCard("Teachers",  "${uiState.teachers.size}",  Icons.Default.School,  Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SuperStatCard("Classes",    "${uiState.classes.size}",    Icons.Default.Class,   Modifier.weight(1f))
            SuperStatCard("Dept Admins","${uiState.deptAdmins.size}", Icons.Default.AdminPanelSettings, Modifier.weight(1f))
        }

        // System Health
        Card(
            colors   = CardDefaults.cardColors(containerColor = Color.White),
            shape    = RoundedCornerShape(16.dp),
            border   = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier          = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("System Health", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("All services operational", color = Color(0xFF10B981), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SuperStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        border   = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandNavy.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = BrandNavy, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                Text(label, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// ── Users tab ─────────────────────────────────────────────────────────────────

@Composable
private fun SuperAdminUsersTab(paddingValues: PaddingValues, uiState: SuperAdminUiState) {
    var selectedUserTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        ScrollableTabRow(
            selectedTabIndex  = selectedUserTab,
            containerColor    = Color.White,
            contentColor      = BrandNavy,
            edgePadding       = 0.dp,
        ) {
            Tab(selected = selectedUserTab == 0, onClick = { selectedUserTab = 0 }, text = { Text("Students (${uiState.students.size})") })
            Tab(selected = selectedUserTab == 1, onClick = { selectedUserTab = 1 }, text = { Text("Teachers (${uiState.teachers.size})") })
            Tab(selected = selectedUserTab == 2, onClick = { selectedUserTab = 2 }, text = { Text("Dept Admins (${uiState.deptAdmins.size})") })
        }

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else when (selectedUserTab) {
                0 -> SuperStudentList(uiState.students)
                1 -> SuperTeacherList(uiState.teachers)
                2 -> SuperDeptAdminList(uiState.deptAdmins)
            }
        }
    }
}

@Composable
private fun SuperStudentList(students: List<StudentProfile>) {
    if (students.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No students.", color = Color.Gray) }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(students, key = { it.id }) { s ->
            SuperUserCard(s.fullName, "Roll: ${s.rollNumber}", "Sem ${s.semester} · Sec ${s.section}", Icons.Default.Person)
        }
    }
}

@Composable
private fun SuperTeacherList(teachers: List<TeacherProfile>) {
    if (teachers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No teachers.", color = Color.Gray) }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(teachers, key = { it.id }) { t ->
            SuperUserCard(t.fullName, "@${t.username}", t.designation.ifBlank { "Faculty" }, Icons.Default.School)
        }
    }
}

@Composable
private fun SuperDeptAdminList(admins: List<DeptAdminProfile>) {
    if (admins.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No dept admins.", color = Color.Gray) }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(admins, key = { it.id }) { a ->
            SuperUserCard(a.fullName, "@${a.username}", "Dept Admin", Icons.Default.AdminPanelSettings)
        }
    }
}

@Composable
private fun SuperUserCard(name: String, identifier: String, badge: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        border   = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(40.dp).clip(CircleShape).background(BrandNavy.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = BrandNavy, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(identifier, fontSize = 12.sp, color = Color.Gray)
            }
            Surface(shape = RoundedCornerShape(100.dp), color = BrandNavy.copy(alpha = 0.08f)) {
                Text(badge, fontSize = 11.sp, color = BrandNavy, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

// ── Classes tab ───────────────────────────────────────────────────────────────

@Composable
private fun SuperAdminClassesTab(paddingValues: PaddingValues, uiState: SuperAdminUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.classes.isEmpty()) {
            Text("No classes.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.classes, key = { it.id }) { group ->
                    SuperClassCard(group)
                }
            }
        }
    }
}

@Composable
private fun SuperClassCard(group: ClassGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

// ── Profile tab ───────────────────────────────────────────────────────────────

@Composable
private fun SuperAdminProfileTab(
    paddingValues    : PaddingValues,
    uiState          : SuperAdminUiState,
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
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BrandNavy)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier         = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.profile?.avatarUrl != null) {
                        AsyncImage(
                            model = uiState.profile!!.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.profile?.fullName ?: "Super Admin", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("@${uiState.profile?.username ?: ""}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(100.dp), color = Color.White.copy(alpha = 0.2f)) {
                    Text("Super Admin", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
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
                SuperProfileInfoRow("Full Name", uiState.profile?.fullName ?: "-")
                SuperProfileInfoRow("Username",  "@${uiState.profile?.username ?: "-"}")
                SuperProfileInfoRow("System Email", uiState.profile?.email ?: "-")
                SuperProfileInfoRow("Status", if (uiState.profile?.isActive == true) "Active" else "Inactive")
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
private fun SuperProfileInfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Black)
    }
    HorizontalDivider(color = Color(0xFFE5E7EB))
}
