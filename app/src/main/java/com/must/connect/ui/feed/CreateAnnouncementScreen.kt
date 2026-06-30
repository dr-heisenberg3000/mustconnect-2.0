package com.must.connect.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.connect.ui.theme.BrandNavy
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import android.provider.OpenableColumns
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnouncementScreen(
    onBack    : () -> Unit,
    viewModel : FeedViewModel = viewModel()
) {
    var title       by remember { mutableStateOf("") }
    var body        by remember { mutableStateOf("") }
    var isPinned    by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by remember { mutableStateOf<String?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachmentUri = uri
        uri?.let {
            // Get file name
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    attachmentName = cursor.getString(nameIndex)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New Announcement",
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F8FA))
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }

            OutlinedTextField(
                value         = title,
                onValueChange = { title = it; errorMessage = null },
                label         = { Text("Announcement Title") },
                placeholder   = { Text("e.g., Week 12 Quiz Reminder") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                singleLine    = true,
                enabled       = !isSubmitting
            )

            OutlinedTextField(
                value         = body,
                onValueChange = { body = it; errorMessage = null },
                label         = { Text("Details") },
                placeholder   = { Text("Share updates, instructions, or links...") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 160.dp),
                shape         = RoundedCornerShape(12.dp),
                enabled       = !isSubmitting
            )

            // Attachment Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Attachment",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (attachmentUri != null) {
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
                                text = attachmentName ?: "Selected File",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                color = BrandNavy
                            )
                            IconButton(onClick = { 
                                attachmentUri = null
                                attachmentName = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { launcher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select File")
                        }
                    }
                }
            }

            // Pin to top toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment   = Alignment.CenterVertically,
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Pin to Top",
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.Black,
                            fontSize   = 15.sp
                        )
                        Text(
                            text     = "Pinned posts appear first in the feed",
                            fontSize = 12.sp,
                            color    = Color.Gray
                        )
                    }
                    Switch(
                        checked         = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors          = SwitchDefaults.colors(checkedTrackColor = BrandNavy)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick   = onBack,
                    modifier  = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape     = RoundedCornerShape(100.dp),
                    enabled   = !isSubmitting
                ) {
                    Text("Cancel", color = BrandNavy, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Please enter an announcement title."
                            return@Button
                        }
                        if (body.isBlank()) {
                            errorMessage = "Please enter the announcement details."
                            return@Button
                        }
                        isSubmitting = true
                        errorMessage = null
                        var attachmentBytes: ByteArray? = null
                        var attachmentType: String? = null
                        
                        if (attachmentUri != null) {
                            try {
                                attachmentBytes = context.contentResolver.openInputStream(attachmentUri!!)?.readBytes()
                                attachmentType = context.contentResolver.getType(attachmentUri!!)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        viewModel.createPost(
                            title   = title,
                            body    = body,
                            pinned  = isPinned,
                            attachmentBytes = attachmentBytes,
                            attachmentName = attachmentName,
                            attachmentType = attachmentType,
                            onSuccess = {
                                isSubmitting = false
                                onBack()
                            },
                            onError = { msg ->
                                isSubmitting = false
                                errorMessage = msg
                            }
                        )
                    },
                    modifier  = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape     = RoundedCornerShape(100.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                    enabled   = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color    = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Publish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
