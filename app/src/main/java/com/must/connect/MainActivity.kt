package com.must.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.must.connect.data.model.UserRole
import com.must.connect.navigation.Screen
import com.must.connect.screens.DeptAdminLoginScreen
import com.must.connect.screens.RoleSelectionScreen
import com.must.connect.screens.SplashScreen
import com.must.connect.screens.StudentLoginScreen
import com.must.connect.screens.SuperAdminLoginScreen
import com.must.connect.screens.TeacherLoginScreen
import com.must.connect.ui.admin.AddUserScreen
import com.must.connect.ui.admin.DeptAdminDashboardScreen
import com.must.connect.ui.admin.SuperAdminDashboardScreen
import com.must.connect.ui.feed.CreateAnnouncementScreen
import com.must.connect.ui.student.StudentDashboardScreen
import com.must.connect.ui.teacher.TeacherDashboardScreen
import com.must.connect.ui.theme.MUSTCONNECTTheme
import com.must.connect.ui.viewmodel.AuthViewModel
import com.must.connect.ui.viewmodel.LoginUiState
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .build()
            
        Coil.setImageLoader(imageLoader)
        
        enableEdgeToEdge()

        setContent {
            MUSTCONNECTTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    val navController = rememberNavController()

                    // One ViewModel instance scoped to the Activity.
                    // All four login screens share it so state is preserved
                    // while navigating between them (e.g., back-press reuse).
                    val authViewModel: AuthViewModel = viewModel()
                    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

                    // ── Global navigation side-effect ──────────────────────────
                    // Reacts to LoginUiState.Success from any login screen and
                    // routes to the correct dashboard, clearing the login stack.
                    LaunchedEffect(uiState) {
                        if (uiState is LoginUiState.Success) {
                            val user  = (uiState as LoginUiState.Success).user
                            val route = authViewModel.dashboardRouteFor(user.role)
                            navController.navigate(route) {
                                // Pop the entire auth flow so back-press exits the app
                                popUpTo(Screen.RoleSelection.route) { inclusive = true }
                            }
                            authViewModel.onNavigationConsumed()
                        }
                    }

                    NavHost(
                        navController    = navController,
                        startDestination = Screen.Splash.route,
                    ) {

                        // ── Splash ────────────────────────────────────────────
                        composable(Screen.Splash.route) {
                            SplashScreen(
                                onSplashComplete = {
                                    navController.navigate(Screen.RoleSelection.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ── Role Selection ────────────────────────────────────
                        composable(Screen.RoleSelection.route) {
                            RoleSelectionScreen(
                                onBack = { navController.popBackStack() },
                                onRoleSelected = { roleName ->
                                    val destination = when (roleName) {
                                        "Student"          -> Screen.StudentLogin.route
                                        "Teacher"          -> Screen.TeacherLogin.route
                                        "Department Admin" -> Screen.DeptAdminLogin.route
                                        "Super Admin"      -> Screen.SuperAdminLogin.route
                                        else               -> return@RoleSelectionScreen
                                    }
                                    navController.navigate(destination)
                                }
                            )
                        }

                        // ── Student Login ─────────────────────────────────────
                        composable(Screen.StudentLogin.route) {
                            StudentLoginScreen(
                                viewModel = authViewModel,
                                role      = UserRole.STUDENT,
                                onBack    = { navController.popBackStack() }
                            )
                        }

                        // ── Teacher Login ─────────────────────────────────────
                        composable(Screen.TeacherLogin.route) {
                            TeacherLoginScreen(
                                viewModel = authViewModel,
                                role      = UserRole.TEACHER,
                                onBack    = { navController.popBackStack() }
                            )
                        }

                        // ── Department Admin Login ────────────────────────────
                        composable(Screen.DeptAdminLogin.route) {
                            DeptAdminLoginScreen(
                                viewModel = authViewModel,
                                role      = UserRole.DEPT_ADMIN,
                                onBack    = { navController.popBackStack() }
                            )
                        }

                        // ── Super Admin Login ─────────────────────────────────
                        composable(Screen.SuperAdminLogin.route) {
                            SuperAdminLoginScreen(
                                viewModel = authViewModel,
                                role      = UserRole.SUPER_ADMIN,
                                onBack    = { navController.popBackStack() }
                            )
                        }

                        // ── Student Dashboard ─────────────────────────────────
                        composable(Screen.StudentDashboard.route) {
                            StudentDashboardScreen(
                                onSignOut = {
                                    authViewModel.onSignOutClicked()
                                    navController.navigate(Screen.RoleSelection.route) {
                                        popUpTo(Screen.StudentDashboard.route) { inclusive = true }
                                    }
                                },
                                onNavigateToChat = {
                                    navController.navigate(Screen.Conversations.route)
                                }
                            )
                        }

                        // ── Teacher Dashboard ─────────────────────────────────
                        composable(Screen.TeacherDashboard.route) {
                            TeacherDashboardScreen(
                                onSignOut = {
                                    authViewModel.onSignOutClicked()
                                    navController.navigate(Screen.RoleSelection.route) {
                                        popUpTo(Screen.TeacherDashboard.route) { inclusive = true }
                                    }
                                },
                                onNavigateToChat = {
                                    navController.navigate(Screen.Conversations.route)
                                }
                            )
                        }

                        // ── Dept Admin Dashboard ──────────────────────────────
                        composable(Screen.DeptAdminDashboard.route) {
                            DeptAdminDashboardScreen(
                                onSignOut = {
                                    authViewModel.onSignOutClicked()
                                    navController.navigate(Screen.RoleSelection.route) {
                                        popUpTo(Screen.DeptAdminDashboard.route) { inclusive = true }
                                    }
                                },
                                onAddUser = {
                                    navController.navigate(Screen.DeptAdminAddUser.route)
                                },
                                onAddAnnouncement = {
                                    navController.navigate(Screen.DeptAdminCreateAnnouncement.route)
                                }
                            )
                        }

                        // ── Dept Admin Add User ───────────────────────────────
                        composable(Screen.DeptAdminAddUser.route) {
                            AddUserScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── Dept Admin Create Announcement ─────────────────────
                        composable(Screen.DeptAdminCreateAnnouncement.route) {
                            CreateAnnouncementScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── Super Admin Dashboard ─────────────────────────────
                        composable(Screen.SuperAdminDashboard.route) {
                            SuperAdminDashboardScreen(
                                onSignOut = {
                                    authViewModel.onSignOutClicked()
                                    navController.navigate(Screen.RoleSelection.route) {
                                        popUpTo(Screen.SuperAdminDashboard.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ── Chat & Conversations ──────────────────────────────
                        composable(Screen.Conversations.route) {
                            val chatViewModel: com.must.connect.ui.chat.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()
                            com.must.connect.ui.chat.ConversationsScreen(
                                uiState = chatUiState,
                                onConversationClick = { partnerId ->
                                    navController.navigate(Screen.Chat.createRoute(partnerId))
                                },
                                onClassGroupClick = { classGroup ->
                                    navController.navigate(Screen.GroupChat.createRoute(classGroup.id))
                                }
                            )
                        }
                        
                        composable(Screen.Chat.route) { backStackEntry ->
                            val partnerId = backStackEntry.arguments?.getString("partnerId") ?: ""
                            val chatViewModel: com.must.connect.ui.chat.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            androidx.compose.runtime.LaunchedEffect(partnerId) {
                                chatViewModel.openChat(partnerId)
                            }
                            androidx.compose.runtime.DisposableEffect(Unit) {
                                onDispose { chatViewModel.closeChat() }
                            }
                            val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()
                            com.must.connect.ui.chat.ChatScreen(
                                uiState = chatUiState,
                                onBack = { navController.popBackStack() },
                                onMessageChange = chatViewModel::onNewMessageChange,
                                onSend = chatViewModel::sendMessage
                            )
                        }

                        composable(Screen.GroupChat.route) { backStackEntry ->
                            val classId = backStackEntry.arguments?.getString("classId") ?: ""
                            val chatViewModel: com.must.connect.ui.chat.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()
                            val group = chatUiState.classGroups.find { it.id == classId }
                            
                            androidx.compose.runtime.LaunchedEffect(group) {
                                if (group != null) {
                                    chatViewModel.openClassChat(group)
                                }
                            }
                            androidx.compose.runtime.DisposableEffect(Unit) {
                                onDispose { chatViewModel.closeClassChat() }
                            }
                            com.must.connect.ui.chat.GroupChatScreen(
                                uiState = chatUiState,
                                onBack = { navController.popBackStack() },
                                onMessageChange = chatViewModel::onNewMessageChange,
                                onSend = chatViewModel::sendMessage
                            )
                        }

                    }
                }
            }
        }
    }
}