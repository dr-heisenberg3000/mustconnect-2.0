package com.must.connect.ui.profile

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.must.connect.data.model.UserRole
import com.must.connect.data.repository.AuthRepository
import com.must.connect.ui.theme.BrandNavy
import kotlinx.coroutines.launch

@Composable
fun EditProfileDialog(
    currentAvatarUrl: String?,
    userId: String,
    role: UserRole,
    onDismiss: () -> Unit
) {
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val authRepository = AuthRepository.getInstance()
    val scope = rememberCoroutineScope()
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Text("Edit Profile Picture", fontWeight = FontWeight.Bold, color = BrandNavy)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
                
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.Gray.copy(alpha=0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUri != null) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "Selected Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (currentAvatarUrl != null) {
                        AsyncImage(
                            model = currentAvatarUrl,
                            contentDescription = "Current Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    }
                }
                
                OutlinedButton(
                    onClick = { launcher.launch("image/*") },
                    enabled = !isUploading
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Select Image")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Image")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUri == null) {
                        onDismiss()
                        return@Button
                    }
                    
                    isUploading = true
                    errorMessage = null
                    
                    scope.launch {
                        try {
                            val bytes = context.contentResolver.openInputStream(selectedUri!!)?.readBytes()
                            if (bytes != null) {
                                var fileName = "avatar.jpg"
                                context.contentResolver.query(selectedUri!!, null, null, null, null)?.use { cursor ->
                                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (cursor.moveToFirst() && nameIndex != -1) {
                                        fileName = cursor.getString(nameIndex)
                                    }
                                }
                                
                                val uploadResult = authRepository.uploadAvatar(bytes, fileName)
                                uploadResult.onSuccess { url ->
                                    authRepository.updateProfileAvatar(userId, role, url)
                                    onDismiss()
                                }.onFailure { e ->
                                    errorMessage = e.message ?: "Failed to upload image."
                                    isUploading = false
                                }
                            } else {
                                errorMessage = "Could not read file."
                                isUploading = false
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "An error occurred."
                            isUploading = false
                        }
                    }
                },
                enabled = !isUploading && selectedUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Upload")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
