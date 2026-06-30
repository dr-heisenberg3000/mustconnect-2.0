package com.must.connect.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.connect.data.model.AuthResult
import com.must.connect.data.model.ClassGroup
import com.must.connect.data.model.ClassPost
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.repository.AuthRepository
import com.must.connect.data.repository.ClassRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentUiState(
    val profile         : StudentProfile?  = null,
    val myClasses       : List<ClassGroup> = emptyList(),
    val selectedClassId : String?          = null,
    val classPosts      : List<ClassPost>  = emptyList(),
    val postAuthors     : Map<String, com.must.connect.data.model.UserProfile> = emptyMap(),
    val classSearchQuery: String           = "",
    val unreadDmCount   : Int              = 0,
    val newPostCount    : Int              = 0,
    val isLoading       : Boolean          = false,
    val feedbackMessage : String?          = null,
    // Password
    val showPasswordDialog    : Boolean = false,
    val newPassword           : String  = "",
    val confirmPassword       : String  = "",
    val passwordChangeLoading : Boolean = false,
    val passwordChangeFeedback: String? = null,
)

class StudentViewModel : ViewModel() {

    private val authRepository  = AuthRepository.getInstance()
    private val classRepository = ClassRepository()
    private val messageRepository = com.must.connect.data.repository.MessageRepository()

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

        init { 
        loadData() 
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthResult.Success) {
                    val p = state.data.profile as? StudentProfile
                    if (p != null) _uiState.update { it.copy(profile = p) }
                }
            }
        }
    }

    fun loadData() {
        val user    = authRepository.currentUser ?: return
        val profile = user.profile as? StudentProfile ?: return
        _uiState.update { it.copy(profile = profile, isLoading = true) }

        viewModelScope.launch {
            classRepository.getClassGroupsForStudent(profile.userId).collect { classes ->
                _uiState.update { it.copy(myClasses = classes, isLoading = false) }
            }
        }
        viewModelScope.launch {
            messageRepository.getUnreadMessageCount(profile.userId).collect { count ->
                _uiState.update { it.copy(unreadDmCount = count) }
            }
        }
    }

    fun selectClass(classId: String?) {
        _uiState.update { it.copy(selectedClassId = classId) }
        if (classId != null) {
            viewModelScope.launch {
                classRepository.getClassPosts(classId).collect { posts ->
                    val authorIds = posts.map { it.authorId }.distinct()
                    val newAuthors = _uiState.value.postAuthors.toMutableMap()
                    val userRepository = com.must.connect.data.repository.UserRepository()
                    authorIds.forEach { aid ->
                        if (!newAuthors.containsKey(aid)) {
                            val profile = userRepository.getTeacherByUserId(aid)
                            if (profile != null) newAuthors[aid] = profile
                        }
                    }
                    _uiState.update { it.copy(classPosts = posts, postAuthors = newAuthors) }
                }
            }
        } else {
            _uiState.update { it.copy(classPosts = emptyList()) }
        }
    }

    fun updateClassSearchQuery(query: String) {
        _uiState.update { it.copy(classSearchQuery = query) }
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

