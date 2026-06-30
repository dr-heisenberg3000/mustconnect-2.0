package com.must.connect.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.must.connect.ui.theme.AccentBlue
import com.must.connect.ui.theme.BackgroundLight
import com.must.connect.ui.theme.DividerColor
import com.must.connect.ui.theme.SubtleGrey
import com.must.connect.ui.theme.TextPrimary
import com.must.connect.ui.theme.TextSecondary

// ── Data model ────────────────────────────────────────────────────────────────

private data class RoleItem(
    val title       : String,
    val description : String,
    val icon        : ImageVector,
)

private val roles = listOf(
    RoleItem(
        title       = "Student",
        description = "Access courses, attendance, examination schedules, and results.",
        icon        = Icons.Default.Person,
    ),
    RoleItem(
        title       = "Teacher",
        description = "Manage classroom activities, grade submissions, and departmental news.",
        icon        = Icons.Default.School,
    ),
    RoleItem(
        title       = "Department Admin",
        description = "Oversee faculty operations, academic calendars, and departmental analytics.",
        icon        = Icons.Default.AccountBalance,
    ),
    RoleItem(
        title       = "Super Admin",
        description = "Full system configuration, security protocols, and global user management.",
        icon        = Icons.Default.Shield,
    ),
)

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Role-selection screen displayed after the splash.  Tapping a card triggers
 * [onRoleSelected] with the canonical role name so the NavHost can route to
 * the correct login screen.
 */
@Composable
fun RoleSelectionScreen(
    onBack        : () -> Unit,
    onRoleSelected: (role: String) -> Unit,
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Login Help") },
            text = { Text("If you are experiencing login issues, please contact the HOD of the CS&IT department for assistance.") },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
    ) {
        LazyColumn(
            contentPadding      = PaddingValues(vertical = 32.dp),
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Text(
                        text       = "Choose Your Role",
                        style      = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight    = FontWeight.Bold,
                            color         = TextPrimary,
                            fontSize      = 26.sp,
                        ),
                        textAlign  = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text      = "Select your identity to access the portal and\npersonalized academic resources.",
                        style     = MaterialTheme.typography.bodyMedium.copy(
                            color      = TextSecondary,
                            lineHeight = 20.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ── Role cards ────────────────────────────────────────────────────
            items(roles) { role ->
                RoleCard(
                    role     = role,
                    onClick  = { onRoleSelected(role.title) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            // ── Help link ─────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick  = { showHelpDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text  = "Need help logging in?",
                        color = AccentBlue,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text      = "v2.0.26 © MUST Technology Department",
                    color     = TextSecondary.copy(alpha = 0.6f),
                    fontSize  = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Role card composable ──────────────────────────────────────────────────────

@Composable
private fun RoleCard(
    role    : RoleItem,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh,
        ),
        label         = "cardScale_${role.title}",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .shadow(
                elevation      = if (isPressed) 2.dp else 6.dp,
                shape          = RoundedCornerShape(16.dp),
                ambientColor   = Color(0x1A1A3BB5),
                spotColor      = Color(0x261A3BB5),
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            ),
        shape   = RoundedCornerShape(16.dp),
        colors  = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(20.dp),
        ) {
            // Icon chip
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(color = SubtleGrey, shape = RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = role.icon,
                    contentDescription = null,
                    tint               = AccentBlue,
                    modifier           = Modifier.size(26.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text block
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = role.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        fontSize   = 16.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = role.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = TextSecondary,
                        lineHeight = 18.sp,
                    ),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Chevron
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint               = DividerColor,
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}
