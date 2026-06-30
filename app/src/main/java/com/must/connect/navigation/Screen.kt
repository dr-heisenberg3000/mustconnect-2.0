package com.must.connect.navigation

/** All navigation destinations used in the NavHost. */
sealed class Screen(val route: String) {
    data object Splash          : Screen("splash")
    data object RoleSelection   : Screen("role_selection")
    data object StudentLogin    : Screen("student_login")
    data object TeacherLogin    : Screen("teacher_login")
    data object DeptAdminLogin  : Screen("dept_admin_login")
    data object SuperAdminLogin : Screen("super_admin_login")

    // ── Post-login dashboards ─────────────────────────────────────────────────
    data object StudentDashboard    : Screen("student_dashboard")
    data object TeacherDashboard    : Screen("teacher_dashboard")
    data object DeptAdminDashboard      : Screen("dept_admin_dashboard")
    data object DeptAdminAddUser        : Screen("dept_admin_add_user")
    data object DeptAdminCreateAnnouncement : Screen("dept_admin_create_announcement")
    data object SuperAdminDashboard : Screen("super_admin_dashboard")

    // Chat routes
    data object Conversations : Screen("conversations")
    data object Chat : Screen("chat/{partnerId}") {
        fun createRoute(partnerId: String) = "chat/$partnerId"
    }
    data object GroupChat : Screen("group_chat/{classId}") {
        fun createRoute(classId: String) = "group_chat/$classId"
    }
}
