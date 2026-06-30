package com.must.connect.ui.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.connect.data.model.AuthResult
import com.must.connect.data.model.ClassGroup
import com.must.connect.data.model.ClassPost
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.model.TeacherProfile
import com.must.connect.data.repository.AuthRepository
import com.must.connect.data.repository.ClassRepository
import com.must.connect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherUiState(
    val profile         : TeacherProfile?    = null,
    val myClasses       : List<ClassGroup>   = emptyList(),
    val selectedClassId : String?            = null,
    val classPosts      : List<ClassPost>    = emptyList(),
    val classMembers    : List<StudentProfile> = emptyList(),
    val isLoading       : Boolean            = false,
    val feedbackMessage : String?            = null,
    val showCreatePostDialog : Boolean = false,
    val newPostTitle         : String  = "",
    val newPostBody          : String  = "",
    val newPostAttachmentUri : android.net.Uri? = null,
    val newPostAttachmentName: String? = null,
    val createPostLoading    : Boolean = false,
    // Password
    val showPasswordDialog    : Boolean = false,
    val newPassword           : String  = "",
    val confirmPassword       : String  = "",
    val passwordChangeLoading : Boolean = false,
    val passwordChangeFeedback: String? = null,
)

class TeacherViewModel : ViewModel() {

    private val authRepository  = AuthRepository.getInstance()
    private val classRepository = ClassRepository()
    private val userRepository  = UserRepository()

    private val _uiState = MutableStateFlow(TeacherUiState())
    val uiState: StateFlow<TeacherUiState> = _uiState.asStateFlow()

        init { 
        loadData() 
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthResult.Success) {
                    val p = state.data.profile as? com.must.connect.data.model.TeacherProfile
                    if (p != null) _uiState.update { it.copy(profile = p) }
                }
            }
        }
    }

    fun loadData() {
        val user    = authRepository.currentUser ?: return
        val profile = user.profile as? TeacherProfile ?: return
        _uiState.update { it.copy(profile = profile) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            classRepository.getClassGroupsForTeacher(profile.userId).collect { classes ->
                _uiState.update { it.copy(myClasses = classes, isLoading = false) }
            }
        }
    }

    fun selectClass(classId: String?) {
        _uiState.update { it.copy(selectedClassId = classId) }
        if (classId != null) {
            loadClassPosts(classId)
            loadClassMembers(classId)
        } else {
            _uiState.update { it.copy(classPosts = emptyList(), classMembers = emptyList()) }
        }
    }

    private fun loadClassPosts(classId: String) {
        viewModelScope.launch {
            classRepository.getClassPosts(classId).collect { posts ->
                _uiState.update { it.copy(classPosts = posts) }
            }
        }
    }

    private fun loadClassMembers(classId: String) {
        viewModelScope.launch {
            classRepository.getClassMembers(classId).collect { memberships ->
                val students = memberships.mapNotNull { m ->
                    userRepository.getStudentById(m.studentId)
                }
                _uiState.update { it.copy(classMembers = students) }
            }
        }
    }

    // ── Create Post ───────────────────────────────────────────────────────────

    fun showCreatePostDialog()  { _uiState.update { it.copy(showCreatePostDialog = true) } }
    fun dismissCreatePostDialog() { _uiState.update { it.copy(showCreatePostDialog = false, newPostTitle = "", newPostBody = "", newPostAttachmentUri = null, newPostAttachmentName = null) } }
    fun onNewPostTitleChange(v: String) { _uiState.update { it.copy(newPostTitle = v) } }
    fun onNewPostBodyChange(v: String)  { _uiState.update { it.copy(newPostBody = v) } }
    fun onAttachmentSelected(uri: android.net.Uri?, name: String?) { _uiState.update { it.copy(newPostAttachmentUri = uri, newPostAttachmentName = name) } }

    fun submitCreatePost(context: android.content.Context) {
        val state   = _uiState.value
        val classId = state.selectedClassId ?: return
        val profile = state.profile ?: return
        if (state.newPostTitle.isBlank() || state.newPostBody.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Title and content are required.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(createPostLoading = true) }
            var attachmentBytes: ByteArray? = null
            var attachmentType: String? = null
            if (state.newPostAttachmentUri != null) {
                try {
                    attachmentBytes = context.contentResolver.openInputStream(state.newPostAttachmentUri)?.readBytes()
                    attachmentType = context.contentResolver.getType(state.newPostAttachmentUri)
                } catch (e: Exception) { e.printStackTrace() }
            }
            val result = classRepository.createClassPost(
                classId  = classId,
                authorId = profile.userId,
                title    = state.newPostTitle.trim(),
                body     = state.newPostBody.trim(),
                attachmentBytes = attachmentBytes,
                attachmentName = state.newPostAttachmentName,
                attachmentType = attachmentType
            )
            result.onSuccess {
                _uiState.update { it.copy(createPostLoading = false, showCreatePostDialog = false, newPostTitle = "", newPostBody = "", newPostAttachmentUri = null, newPostAttachmentName = null, feedbackMessage = "Post published!") }
                loadClassPosts(classId)
            }.onFailure { e ->
                _uiState.update { it.copy(createPostLoading = false, feedbackMessage = e.message ?: "Failed to post.") }
            }
        }
    }

    fun deletePost(postId: String) {
        val classId = _uiState.value.selectedClassId ?: return
        viewModelScope.launch {
            classRepository.deleteClassPost(postId).onSuccess {
                loadClassPosts(classId)
            }.onFailure { e ->
                _uiState.update { it.copy(feedbackMessage = e.message ?: "Failed to delete post.") }
            }
        }
    }


    // ── Password ──────────────────────────────────────────────────────────────

    fun showPasswordDialog()  { _uiState.update { it.copy(showPasswordDialog = true) } }
    fun dismissPasswordDialog() { _uiState.update { it.copy(showPasswordDialog = false, newPassword = "", confirmPassword = "", passwordChangeFeedback = null) } }
    fun onNewPasswordChange(v: String)     { _uiState.update { it.copy(newPassword = v, passwordChangeFeedback = null) } }
    fun onConfirmPasswordChange(v: String) { _uiState.update { it.copy(confirmPassword = v, passwordChangeFeedback = null) } }

    fun submitPasswordChange() {
        val state = _uiState.value
        if (state.newPassword.length < 6) { _uiState.update { it.copy(passwordChangeFeedback = "Password must be at least 6 characters.") }; return }
        if (state.newPassword != state.confirmPassword) { _uiState.update { it.copy(passwordChangeFeedback = "Passwords do not match.") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(passwordChangeLoading = true) }
            val result = authRepository.changeOwnPassword(state.newPassword)
            when (result) {
                is AuthResult.Success -> _uiState.update { it.copy(passwordChangeLoading = false, showPasswordDialog = false, feedbackMessage = "Password changed!") }
                is AuthResult.Error   -> _uiState.update { it.copy(passwordChangeLoading = false, passwordChangeFeedback = result.message) }
                else -> {}
            }
        }
    }

    fun clearFeedback() { _uiState.update { it.copy(feedbackMessage = null) } }
}

