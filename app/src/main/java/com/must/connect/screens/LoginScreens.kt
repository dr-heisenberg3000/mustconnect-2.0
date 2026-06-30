package com.must.connect.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.must.connect.data.model.UserRole
import com.must.connect.ui.theme.AccentBlue
import com.must.connect.ui.theme.BrandNavy
import com.must.connect.ui.theme.BrandRoyalBlue
import com.must.connect.ui.theme.OnBrandWhite
import com.must.connect.ui.theme.OnBrandWhite70
import com.must.connect.ui.viewmodel.AuthViewModel
import com.must.connect.ui.viewmodel.LoginUiState

// ── Shared login screen ────────────────────────────────────────────────────────

/**
 * Common login form used by all four role screens.
 *
 * Layout:
 *  - Deep gradient header with role icon & title
 *  - Identifier field (roll number / username label adapts to role)
 *  - Password field with show/hide toggle
 *  - Inline error banner (animated)
 *  - Login button with loading spinner state
 */
@Composable
private fun LoginScreenContent(
    role            : UserRole,
    roleTitle       : String,
    roleSubtitle    : String,
    icon            : ImageVector,
    viewModel       : AuthViewModel,
    onBack          : () -> Unit = {},
) {
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val isLoading     = uiState is LoginUiState.Loading
    val errorMessage  = (uiState as? LoginUiState.Error)?.message
    val identifierLabel = if (role == UserRole.STUDENT) "Roll Number" else "Username"

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Gradient header ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(BrandNavy, BrandRoyalBlue, AccentBlue))
                    )
                    .padding(top = 52.dp, bottom = 36.dp, start = 24.dp, end = 24.dp),
            ) {
                // Back arrow
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = OnBrandWhite,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = null,
                            tint               = OnBrandWhite,
                            modifier           = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text       = roleTitle,
                        color      = OnBrandWhite,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text      = roleSubtitle,
                        color     = OnBrandWhite70,
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }
            }

            // ── Form ──────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F7FA))
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            ) {

                // Identifier field
                OutlinedTextField(
                    value         = formState.identifier,
                    onValueChange = viewModel::onIdentifierChange,
                    label         = { Text(identifierLabel) },
                    singleLine    = true,
                    enabled       = !isLoading,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentBlue,
                        focusedLabelColor    = AccentBlue,
                        cursorColor          = AccentBlue,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password field
                OutlinedTextField(
                    value         = formState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label         = { Text("Password") },
                    singleLine    = true,
                    enabled       = !isLoading,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    visualTransformation = if (formState.isPasswordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.onLoginClicked(role)
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (formState.isPasswordVisible)
                                    Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (formState.isPasswordVisible)
                                    "Hide password" else "Show password",
                                tint = AccentBlue,
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentBlue,
                        focusedLabelColor    = AccentBlue,
                        cursorColor          = AccentBlue,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Inline error banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter   = fadeIn(),
                    exit    = fadeOut(),
                ) {
                    Text(
                        text      = errorMessage ?: "",
                        color     = MaterialTheme.colorScheme.error,
                        style     = MaterialTheme.typography.bodySmall,
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Login button
                Button(
                    onClick  = {
                        focusManager.clearFocus()
                        viewModel.onLoginClicked(role)
                    },
                    enabled  = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(22.dp),
                            color       = Color.White,
                            strokeWidth = 2.5.dp,
                            strokeCap   = StrokeCap.Round,
                        )
                    } else {
                        Text(
                            text       = "Sign In",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// ── Four public composables ────────────────────────────────────────────────────

@Composable
fun StudentLoginScreen(
    viewModel: AuthViewModel,
    role     : UserRole,
    onBack   : () -> Unit = {},
) {
    LoginScreenContent(
        role         = role,
        roleTitle    = "Student Login",
        roleSubtitle = "Enter your roll number and password\nto access your classes.",
        icon         = Icons.Default.Person,
        viewModel    = viewModel,
        onBack       = onBack,
    )
}

@Composable
fun TeacherLoginScreen(
    viewModel: AuthViewModel,
    role     : UserRole,
    onBack   : () -> Unit = {},
) {
    LoginScreenContent(
        role         = role,
        roleTitle    = "Teacher Login",
        roleSubtitle = "Enter your username and password\nto manage your classes.",
        icon         = Icons.Default.School,
        viewModel    = viewModel,
        onBack       = onBack,
    )
}

@Composable
fun DeptAdminLoginScreen(
    viewModel: AuthViewModel,
    role     : UserRole,
    onBack   : () -> Unit = {},
) {
    LoginScreenContent(
        role         = role,
        roleTitle    = "Department Admin Login",
        roleSubtitle = "Enter your credentials\nto access administrative controls.",
        icon         = Icons.Default.AccountBalance,
        viewModel    = viewModel,
        onBack       = onBack,
    )
}

@Composable
fun SuperAdminLoginScreen(
    viewModel: AuthViewModel,
    role     : UserRole,
    onBack   : () -> Unit = {},
) {
    LoginScreenContent(
        role         = role,
        roleTitle    = "Super Admin Login",
        roleSubtitle = "Authorised personnel only.\nSystem-level access.",
        icon         = Icons.Default.Shield,
        viewModel    = viewModel,
        onBack       = onBack,
    )
}
