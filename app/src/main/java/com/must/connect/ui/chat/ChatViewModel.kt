package com.must.connect.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.connect.data.model.DirectMessage
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.model.TeacherProfile
import com.must.connect.data.model.UserProfile
import com.must.connect.data.repository.AuthRepository
import com.must.connect.data.repository.MessageRepository
import com.must.connect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val currentUserProfile : UserProfile? = null,
    // Conversations List State
    val conversations      : List<DirectMessage> = emptyList(),
    val partners           : Map<String, UserProfile> = emptyMap(), // Maps partnerId -> Profile
    val isConversationsLoading: Boolean = false,
    
    // Active Chat State
    val activePartnerId    : String? = null,
    val activePartnerProfile: UserProfile? = null,
    val activeMessages     : List<DirectMessage> = emptyList(),
    val isChatLoading      : Boolean = false,
    val newMessageText     : String = "",
    val isSending          : Boolean = false,
    val error              : String? = null,
    
    // Search state
    val showSearch         : Boolean = false,
    val searchQuery        : String = "",

    // Contacts list
    val contacts           : List<UserProfile> = emptyList(),
    val isContactsLoading  : Boolean = false,
    
    // Groups list
    val classGroups        : List<com.must.connect.data.model.ClassGroup> = emptyList(),
    val isClassGroupsLoading: Boolean = false,
    
    // Active Group Chat
    val activeClassGroup   : com.must.connect.data.model.ClassGroup? = null,
    val activeClassMessages: List<com.must.connect.data.model.ClassMessage> = emptyList(),
)

class ChatViewModel : ViewModel() {
    private val authRepository    = AuthRepository.getInstance()
    private val messageRepository = MessageRepository()
    private val userRepository    = UserRepository()
    private val classRepository   = com.must.connect.data.repository.ClassRepository()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is com.must.connect.data.model.AuthResult.Success) {
                    _uiState.update { it.copy(currentUserProfile = state.data.profile) }
                }
            }
        }
    }

    private fun loadCurrentUser() {
        val user = authRepository.currentUser ?: return
        _uiState.update { it.copy(currentUserProfile = user.profile) }
        loadConversations(user.profile.userId)
        loadClassGroups(user.profile)
    }

    private fun loadClassGroups(profile: UserProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClassGroupsLoading = true) }
            if (profile is com.must.connect.data.model.StudentProfile) {
                classRepository.getClassGroupsForStudent(profile.userId).collect { groups ->
                    _uiState.update { it.copy(classGroups = groups, isClassGroupsLoading = false) }
                }
            } else if (profile is com.must.connect.data.model.TeacherProfile) {
                classRepository.getClassGroupsForTeacher(profile.userId).collect { groups ->
                    _uiState.update { it.copy(classGroups = groups, isClassGroupsLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isClassGroupsLoading = false) }
            }
        }
    }

    fun loadConversations(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConversationsLoading = true) }
            messageRepository.getConversationPartners(userId).collect { allMessages ->
                // Deduplicate to find unique partners
                val partnerIds = allMessages.map { 
                    if (it.senderId == userId) it.receiverId else it.senderId 
                }.distinct()
                
                // Fetch profiles for partners
                val profiles = mutableMapOf<String, UserProfile>()
                partnerIds.forEach { pid ->
                    // Could be student or teacher
                    val student = userRepository.getStudentByUserId(pid)
                    if (student != null) {
                        profiles[pid] = student
                    } else {
                        val teacher = userRepository.getTeacherByUserId(pid)
                        if (teacher != null) profiles[pid] = teacher
                    }
                }

                // Get latest message per partner
                val latestMessages = partnerIds.mapNotNull { pid ->
                    allMessages.firstOrNull { it.senderId == pid || it.receiverId == pid }
                }

                _uiState.update { 
                    it.copy(
                        conversations = latestMessages.sortedByDescending { msg -> msg.createdAt },
                        partners = profiles,
                        isConversationsLoading = false
                    ) 
                }
            }
        }
    }

    fun loadContacts() {
        val user = _uiState.value.currentUserProfile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isContactsLoading = true) }
            
            val classFlow = if (user is com.must.connect.data.model.StudentProfile) {
                classRepository.getClassGroupsForStudent(user.userId)
            } else {
                classRepository.getClassGroupsForTeacher(user.userId)
            }
            
            val groups = classFlow.first()
            val contactsList = mutableSetOf<UserProfile>()
            
            val teacherIdsToFetch = mutableSetOf<String>()
            val studentIdsToFetch = mutableSetOf<String>()
            
            for (group in groups) {
                // Add teacher
                if (group.teacherId != null && group.teacherId != user.userId) {
                    teacherIdsToFetch.add(group.teacherId)
                }
                
                // Add students
                val memberships = classRepository.getClassMembers(group.id).first()
                for (m in memberships) {
                    if (m.studentId != user.userId) {
                        studentIdsToFetch.add(m.studentId)
                    }
                }
            }
            
            if (teacherIdsToFetch.isNotEmpty()) {
                val teachers = userRepository.getTeachersByUserIds(teacherIdsToFetch.toList())
                contactsList.addAll(teachers)
            }
            
            if (studentIdsToFetch.isNotEmpty()) {
                val students = userRepository.getStudentsByUserIds(studentIdsToFetch.toList())
                contactsList.addAll(students)
            }
            
            _uiState.update { it.copy(contacts = contactsList.toList(), isContactsLoading = false) }
        }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(showSearch = !it.showSearch, searchQuery = if (it.showSearch) "" else it.searchQuery) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openChat(partnerId: String) {
        val currentUserId = _uiState.value.currentUserProfile?.userId ?: return
        
        var partnerProfile = _uiState.value.partners[partnerId]
        if (partnerProfile == null) {
            partnerProfile = _uiState.value.contacts.find { it.userId == partnerId }
        }
        
        _uiState.update { it.copy(activePartnerId = partnerId, activePartnerProfile = partnerProfile, isChatLoading = true) }
        
        viewModelScope.launch {
            messageRepository.getConversation(currentUserId, partnerId).collect { msgs ->
                _uiState.update { it.copy(activeMessages = msgs, isChatLoading = false) }
                // Mark as read
                messageRepository.markMessagesRead(userId = currentUserId, senderId = partnerId)
            }
        }
    }

    fun closeChat() {
        _uiState.update { it.copy(activePartnerId = null, activePartnerProfile = null, activeMessages = emptyList()) }
    }

    fun openClassChat(group: com.must.connect.data.model.ClassGroup) {
        _uiState.update { it.copy(activeClassGroup = group, isChatLoading = true) }
        viewModelScope.launch {
            messageRepository.getClassMessages(group.id).collect { msgs ->
                // also load sender profiles for these messages so we can show names/avatars in UI.
                // We'll just fetch partners mapping
                val senderIds = msgs.map { it.senderId }.distinct()
                val newPartners = _uiState.value.partners.toMutableMap()
                senderIds.forEach { sid ->
                    if (!newPartners.containsKey(sid)) {
                        val student = userRepository.getStudentByUserId(sid)
                        if (student != null) newPartners[sid] = student
                        else {
                            val teacher = userRepository.getTeacherByUserId(sid)
                            if (teacher != null) newPartners[sid] = teacher
                        }
                    }
                }
                _uiState.update { it.copy(activeClassMessages = msgs, isChatLoading = false, partners = newPartners) }
            }
        }
    }

    fun closeClassChat() {
        _uiState.update { it.copy(activeClassGroup = null, activeClassMessages = emptyList()) }
    }

    fun onNewMessageChange(text: String) {
        _uiState.update { it.copy(newMessageText = text) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val senderId = state.currentUserProfile?.userId ?: return
        val text = state.newMessageText.trim()
        if (text.isEmpty()) return

        if (state.activeClassGroup != null) {
            // Send class message
            val classId = state.activeClassGroup.id
            viewModelScope.launch {
                _uiState.update { it.copy(isSending = true, newMessageText = "") }
                val result = messageRepository.sendClassMessage(classId, senderId, text)
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message, newMessageText = text) }
                }
                _uiState.update { it.copy(isSending = false) }
            }
        } else if (state.activePartnerId != null) {
            // Send direct message
            val receiverId = state.activePartnerId
            viewModelScope.launch {
                _uiState.update { it.copy(isSending = true, newMessageText = "") }
                val result = messageRepository.sendMessage(senderId, receiverId, text)
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message, newMessageText = text) }
                }
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}


