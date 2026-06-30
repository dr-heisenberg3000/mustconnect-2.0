package com.must.connect.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.connect.data.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    viewModel: AddUserViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val primaryDark   = Color(0xFF001358)
    val backgroundLight = Color(0xFFF7F8FA)
    val cardBorder    = Color(0xFFE5E7EB)

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add New User",
                        fontWeight = FontWeight.Bold,
                        color = primaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundLight)
            )
        },
        containerColor = backgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Create New Account",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Role Selection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Account Role",
                        fontWeight = FontWeight.SemiBold,
                        color = primaryDark,
                        fontSize = 16.sp
                    )

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = when (uiState.role) {
                                UserRole.STUDENT     -> "Student"
                                UserRole.TEACHER     -> "Teacher"
                                UserRole.DEPT_ADMIN  -> "Department Admin"
                                UserRole.SUPER_ADMIN -> "Super Admin"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Role") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // Only Dept Admin can create Students, Teachers
                            // Only Super Admin can create Dept Admin, Super Admin
                            val availableRoles = mutableListOf(
                                UserRole.STUDENT     to "Student",
                                UserRole.TEACHER     to "Teacher"
                            )
                            
                            if (viewModel.currentUserRole == UserRole.SUPER_ADMIN) {
                                availableRoles.add(UserRole.DEPT_ADMIN to "Department Admin")
                                availableRoles.add(UserRole.SUPER_ADMIN to "Super Admin")
                            }

                            availableRoles.forEach { (role, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.onRoleChange(role)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Identity & Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Identity & Details",
                        fontWeight = FontWeight.SemiBold,
                        color = primaryDark,
                        fontSize = 16.sp
                    )

                    // Full Name
                    OutlinedTextField(
                        value = uiState.fullName,
                        onValueChange = viewModel::onFullNameChange,
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g., Ahmed Khan") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !uiState.isLoading
                    )

                    // Role-specific identifier
                    val identifierLabel = when (uiState.role) {
                        UserRole.STUDENT -> "Roll Number"
                        else             -> "Username"
                    }
                    val identifierPlaceholder = when (uiState.role) {
                        UserRole.STUDENT -> "e.g., BSCS-001"
                        UserRole.TEACHER -> "e.g., sarah.jenkins"
                        else             -> "e.g., admin"
                    }

                    OutlinedTextField(
                        value = uiState.identifier,
                        onValueChange = viewModel::onIdentifierChange,
                        label = { Text(identifierLabel) },
                        placeholder = { Text(identifierPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !uiState.isLoading
                    )

                    // Teacher: Designation
                    if (uiState.role == UserRole.TEACHER) {
                        OutlinedTextField(
                            value = uiState.designation,
                            onValueChange = viewModel::onDesignationChange,
                            label = { Text("Designation") },
                            placeholder = { Text("e.g., Lecturer, Assistant Professor") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !uiState.isLoading
                        )
                    }

                    // Student: Section + Semester
                    if (uiState.role == UserRole.STUDENT) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.section,
                                onValueChange = viewModel::onSectionChange,
                                label = { Text("Section") },
                                placeholder = { Text("A") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                enabled = !uiState.isLoading
                            )
                            OutlinedTextField(
                                value = uiState.semester,
                                onValueChange = viewModel::onSemesterChange,
                                label = { Text("Semester") },
                                placeholder = { Text("1-8") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                enabled = !uiState.isLoading,
                                leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            // Security Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Security",
                        fontWeight = FontWeight.SemiBold,
                        color = primaryDark,
                        fontSize = 16.sp
                    )

                    var showPassword by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Initial Password") },
                        placeholder = { Text("Min. 6 characters") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "Hide" else "Show", fontSize = 12.sp)
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !uiState.isLoading
                    )

                    // Email preview hint
                    val previewEmail = if (uiState.identifier.isNotBlank()) {
                        "System email: ${
                            when (uiState.role) {
                                UserRole.STUDENT     -> "${uiState.identifier.lowercase()}@student.must.edu.pk"
                                UserRole.TEACHER     -> "${uiState.identifier.lowercase()}@teacher.must.edu.pk"
                                UserRole.DEPT_ADMIN  -> "${uiState.identifier.lowercase()}@deptadmin.must.edu.pk"
                                UserRole.SUPER_ADMIN -> "${uiState.identifier.lowercase()}@superadmin.must.edu.pk"
                            }
                        }"
                    } else null

                    if (previewEmail != null) {
                        Text(
                            text = previewEmail,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Create Button
            Button(
                onClick = { viewModel.onCreateUserClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDark),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
