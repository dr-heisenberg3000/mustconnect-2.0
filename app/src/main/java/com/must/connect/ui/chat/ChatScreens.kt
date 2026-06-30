package com.must.connect.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Class
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.must.connect.data.model.DirectMessage
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.model.TeacherProfile
import com.must.connect.data.model.UserProfile
import com.must.connect.ui.theme.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    uiState: ChatUiState,
    onConversationClick: (String) -> Unit,
    onClassGroupClick: (com.must.connect.data.model.ClassGroup) -> Unit,
) {
    var showContactsDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showNotificationsDropdown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedTabIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ChatViewModel>()

    val currentUserId = uiState.currentUserProfile?.userId ?: ""

    // Filter conversations based on search
    val filteredConversations = androidx.compose.runtime.remember(uiState.conversations, uiState.partners, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.conversations
        } else {
            uiState.conversations.filter { msg ->
                val partnerId = if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
                val partner = uiState.partners[partnerId]
                partner?.fullName?.contains(uiState.searchQuery, ignoreCase = true) == true ||
                msg.body.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Filter class groups based on search
    val filteredGroups = androidx.compose.runtime.remember(uiState.classGroups, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.classGroups
        } else {
            uiState.classGroups.filter { group ->
                group.name.contains(uiState.searchQuery, ignoreCase = true) ||
                group.subject.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    val unreadMessages = uiState.conversations.filter { !it.isRead && it.receiverId == currentUserId }

    androidx.compose.runtime.LaunchedEffect(showContactsDialog) {
        if (showContactsDialog && uiState.contacts.isEmpty()) {
            viewModel.loadContacts()
        }
    }

    if (showContactsDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showContactsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "New Conversation",
                        color = BrandNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                    
                    if (uiState.isContactsLoading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentBlue)
                        }
                    } else if (uiState.contacts.isEmpty()) {
                        Text("No contacts found.", modifier = Modifier.padding(24.dp))
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                            items(uiState.contacts) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showContactsDialog = false
                                            onConversationClick(contact.userId)
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray)) {
                                        if (contact.avatarUrl != null) {
                                            coil.compose.AsyncImage(
                                                model = contact.avatarUrl,
                                                contentDescription = "Avatar",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.Gray, modifier = Modifier.size(24.dp).align(Alignment.Center))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = contact.fullName,
                                        fontSize = 16.sp,
                                        color = BrandNavy,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                HorizontalDivider(color = DividerColor)
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showContactsDialog = false }) {
                            Text("Close", color = BrandNavy)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState.showSearch) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search chats...", color = Color.Gray) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("MUST-CONNECT", color = BrandNavy, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(if (uiState.showSearch) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search", tint = BrandNavy)
                    }
                    Box {
                        IconButton(onClick = { showNotificationsDropdown = true }) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = BrandNavy)
                            if (unreadMessages.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.Red, CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showNotificationsDropdown,
                            onDismissRequest = { showNotificationsDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            if (unreadMessages.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No new notifications", color = Color.Gray) },
                                    onClick = { showNotificationsDropdown = false }
                                )
                            } else {
                                unreadMessages.forEach { msg ->
                                    val partner = uiState.partners[msg.senderId]
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(partner?.fullName ?: "Someone", fontWeight = FontWeight.Bold, color = BrandNavy)
                                                Text(msg.body, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.DarkGray, fontSize = 12.sp)
                                            }
                                        },
                                        onClick = { 
                                            showNotificationsDropdown = false
                                            onConversationClick(msg.senderId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showContactsDialog = true },
                containerColor = BrandNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "New Chat")
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Pill Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Chats", "Groups").forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) BrandNavy else SubtleGrey)
                                .clickable { selectedTabIndex = index }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> {
                            if (uiState.isConversationsLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = AccentBlue)
                                }
                            } else if (filteredConversations.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(if (uiState.searchQuery.isNotEmpty()) "No matches found." else "No messages yet.", color = TextSecondary, fontSize = 16.sp)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filteredConversations) { msg ->
                                        val partnerId = if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
                                        val partner = uiState.partners[partnerId]
                                        if (partner != null) {
                                            ConversationItem(
                                                message = msg,
                                                partner = partner,
                                                currentUserId = currentUserId,
                                                onClick = { onConversationClick(partnerId) }
                                            )
                                        }
                                    }
                                    item {
                                        AcademicSecurityCard()
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (uiState.isClassGroupsLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = AccentBlue)
                                }
                            } else if (filteredGroups.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(if (uiState.searchQuery.isNotEmpty()) "No matches found." else "No groups available.", color = TextSecondary, fontSize = 16.sp)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filteredGroups) { group ->
                                        ClassGroupItem(
                                            group = group,
                                            onClick = { onClassGroupClick(group) }
                                        )
                                    }
                                    item {
                                        AcademicSecurityCard()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AcademicSecurityCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EEFC)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(Icons.Default.Security, contentDescription = "Security", tint = BrandNavy, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Academic Security", color = BrandNavy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "All communication within MUST-CONNECT is encrypted according to institutional protocols for Fall 2026.",
                color = BrandNavy.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ConversationItem(
    message: DirectMessage,
    partner: UserProfile,
    currentUserId: String,
    onClick: () -> Unit
) {
    val isUnread = !message.isRead && message.receiverId == currentUserId
    
    val timeStr = try {
        val f = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault())
        f.format(Instant.parse(message.createdAt))
    } catch (e: Exception) { "" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SubtleGrey),
            contentAlignment = Alignment.Center
        ) {
            if (!partner.avatarUrl.isNullOrEmpty()) {
                coil.compose.AsyncImage(
                    model = partner.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.Gray, modifier = Modifier.size(32.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = partner.fullName,
                    color = BrandNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = timeStr.ifEmpty { "10:42 AM" },
                    color = BrandNavy,
                    fontSize = 12.sp,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (if (message.senderId == currentUserId) "You: " else "") + message.body,
                    color = if (isUnread) BrandNavy else TextSecondary,
                    maxLines = 2,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal
                )
                if (isUnread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(BrandNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(start = 88.dp, end = 16.dp))
}

@Composable
private fun ClassGroupItem(
    group: com.must.connect.data.model.ClassGroup,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandNavy.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Class, contentDescription = "Class", tint = BrandNavy, modifier = Modifier.size(28.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                color = BrandNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${group.subject} - Section ${group.section}",
                color = TextSecondary,
                maxLines = 1,
                fontSize = 14.sp
            )
        }
    }
    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(start = 88.dp, end = 16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onBack: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val partner = uiState.activePartnerProfile
                    if (partner != null) {
                        if (!partner.avatarUrl.isNullOrEmpty()) {
                            coil.compose.AsyncImage(
                                model = partner.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(SubtleGrey),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(partner?.fullName ?: "Chat", color = BrandNavy, fontWeight = FontWeight.Bold)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isChatLoading) {
                CircularProgressIndicator(color = BrandNavy, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    reverseLayout = false
                ) {
                    items(uiState.activeMessages) { msg ->
                        val isOwn = msg.senderId == uiState.currentUserProfile?.userId
                        val profile = if (isOwn) uiState.currentUserProfile else uiState.activePartnerProfile
                        ChatMessageItem(
                            message = msg,
                            isOwn = isOwn,
                            profile = profile
                        )
                    }
                }
            }
        }

        // Input Area
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.newMessageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = DividerColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = BrandNavy,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onSend,
                    enabled = uiState.newMessageText.isNotBlank() && !uiState.isSending,
                    modifier = Modifier.background(BrandNavy, CircleShape)
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    uiState: ChatUiState,
    onBack: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        TopAppBar(
            title = { Text(uiState.activeClassGroup?.name ?: "Group Chat", color = BrandNavy, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isChatLoading) {
                CircularProgressIndicator(color = BrandNavy, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    reverseLayout = false
                ) {
                    items(uiState.activeClassMessages) { msg ->
                        val isOwn = msg.senderId == uiState.currentUserProfile?.userId
                        val partner = uiState.partners[msg.senderId]
                        val profile = if (isOwn) uiState.currentUserProfile else partner
                        GroupChatMessageItem(
                            message = msg,
                            profile = profile,
                            isOwn = isOwn
                        )
                    }
                }
            }
        }

        // Input Area
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.newMessageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandNavy,
                        unfocusedBorderColor = DividerColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = BrandNavy,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onSend,
                    enabled = uiState.newMessageText.isNotBlank() && !uiState.isSending,
                    modifier = Modifier.background(BrandNavy, CircleShape)
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: DirectMessage, isOwn: Boolean, profile: UserProfile?) {
    val df = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val dateStr = try { df.format(Date.from(java.time.Instant.parse(message.createdAt))) } catch (e: Exception) { "" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (!isOwn) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SubtleGrey),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile?.avatarUrl.isNullOrEmpty()) {
                        coil.compose.AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwn) 16.dp else 4.dp,
                        bottomEnd = if (isOwn) 4.dp else 16.dp
                    ))
                    .background(if (isOwn) BrandNavy else Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.body,
                    color = if (isOwn) Color.White else TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
            if (isOwn) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SubtleGrey),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile?.avatarUrl.isNullOrEmpty()) {
                        coil.compose.AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateStr,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(
                start = if (isOwn) 0.dp else 40.dp,
                end = if (isOwn) 40.dp else 0.dp
            )
        )
    }
}

@Composable
private fun GroupChatMessageItem(
    message: com.must.connect.data.model.ClassMessage,
    profile: UserProfile?,
    isOwn: Boolean
) {
    val df = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val dateStr = try { df.format(Date.from(java.time.Instant.parse(message.createdAt))) } catch (e: Exception) { "" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        if (!isOwn) {
            val name = profile?.fullName ?: "Unknown User"
            val additionalInfo = when (profile) {
                is StudentProfile -> profile.rollNumber.takeIf { it.isNotBlank() }
                is TeacherProfile -> profile.designation.takeIf { it.isNotBlank() }
                else -> null
            }
            val titleText = if (additionalInfo != null) "$name • $additionalInfo" else name

            Text(
                text = titleText,
                color = BrandNavy,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp, start = 40.dp)
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            if (!isOwn) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SubtleGrey),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile?.avatarUrl.isNullOrEmpty()) {
                        coil.compose.AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwn) 16.dp else 4.dp,
                        bottomEnd = if (isOwn) 4.dp else 16.dp
                    ))
                    .background(if (isOwn) BrandNavy else Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.body,
                    color = if (isOwn) Color.White else TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
            if (isOwn) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SubtleGrey),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile?.avatarUrl.isNullOrEmpty()) {
                        coil.compose.AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateStr,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(
                start = if (isOwn) 0.dp else 40.dp,
                end = if (isOwn) 40.dp else 0.dp
            )
        )
    }
}
