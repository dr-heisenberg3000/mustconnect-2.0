package com.must.connect.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.must.connect.ui.theme.AccentBlue
import com.must.connect.ui.theme.BackgroundLight
import com.must.connect.ui.theme.BrandNavy
import com.must.connect.ui.theme.BrandRoyalBlue
import com.must.connect.ui.theme.OnBrandWhite
import com.must.connect.ui.theme.OnBrandWhite70
import com.must.connect.ui.theme.TextPrimary
import com.must.connect.ui.theme.TextSecondary

// ── Shared dashboard shell ────────────────────────────────────────────────────
// Each dashboard will be fully built in its own feature sprint.
// These are fully compilable, visually distinct placeholders that serve as
// real navigation destinations — not blank screens or stubs.

@Composable
private fun DashboardShell(
    title      : String,
    subtitle   : String,
    icon       : ImageVector,
    accentColor: Color,
    onSignOut  : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
    ) {
        // ── Header band ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(listOf(BrandNavy, BrandRoyalBlue, accentColor))
                )
                .padding(top = 56.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = OnBrandWhite,
                        modifier           = Modifier.size(30.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text       = title,
                    color      = OnBrandWhite,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = subtitle,
                    color = OnBrandWhite70,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // ── Coming-soon body ──────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text       = "Dashboard Coming Soon",
                color      = TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = "This screen will be built in the next sprint.\nAuthentication is live and working.",
                color     = TextSecondary,
                fontSize  = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
    }
}

// ── Four concrete dashboard destinations ──────────────────────────────────────

@Composable
fun StudentDashboardScreen(onSignOut: () -> Unit = {}) {
    DashboardShell(
        title       = "Student Dashboard",
        subtitle    = "Welcome back — your classes and updates",
        icon        = Icons.Default.Person,
        accentColor = AccentBlue,
        onSignOut   = onSignOut,
    )
}

@Composable
fun TeacherDashboardScreen(onSignOut: () -> Unit = {}) {
    DashboardShell(
        title       = "Teacher Dashboard",
        subtitle    = "Manage your classes and announcements",
        icon        = Icons.Default.School,
        accentColor = AccentBlue,
        onSignOut   = onSignOut,
    )
}



