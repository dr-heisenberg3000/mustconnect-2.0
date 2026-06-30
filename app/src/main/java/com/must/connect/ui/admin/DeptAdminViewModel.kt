package com.must.connect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.connect.data.model.ClassGroup
import com.must.connect.data.model.DeptAdminProfile
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.model.TeacherProfile
import com.must.connect.data.model.AuthResult
import com.must.connect.data.repository.AuthRepository
import com.must.connect.data.repository.ClassRepository
import com.must.connect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeptAdminUiState(
    val profile         : DeptAdminProfile?  = null,
    val students        : List<StudentProfile> = emptyList(),
    val teachers        : List<TeacherProfile> = emptyList(),
    val classes         : List<ClassGroup>   = emptyList(),
    val classMemberships: Map<String, List<String>> = emptyMap(),
    val isLoading       : Boolean            = false,
    val feedbackMessage : String?            = null,
    val isError         : Boolean            = false,
    // Password change
    val showPasswordDialog    : Boolean = false,
    val newPassword           : String  = "",
    val confirmPassword       : String  = "",
    val passwordChangeLoading : Boolean = false,
    val passwordChangeFeedback: String? = null,
    // Create class dialog
    val showCreateClassDialog : Boolean = false,
    val newClassName    : String  = "",
    val newSubject      : String  = "",
    val newSection      : String  = "",
    val newSemester     : String  = "",
    val createClassLoading: Boolean = false,
)

class DeptAdminViewModel : ViewModel() {

    private val authRepository  = AuthRepository.getInstance()
    private val userRepository  = UserRepository()
    private val classRepository = ClassRepository()

    private val _uiState = MutableStateFlow(DeptAdminUiState())
    val uiState: StateFlow<DeptAdminUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        val user = authRepository.currentUser ?: return
        val profile = user.profile as? DeptAdminProfile
        _uiState.update { it.copy(profile = profile) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load students
            userRepository.getAllStudents().collect { students ->
                _uiState.update { it.copy(students = students) }
            }
        }
        viewModelScope.launch {
            userRepository.getAllTeachers().collect { teachers ->
                _uiState.update { it.copy(teachers = teachers) }
            }
        }
        viewModelScope.launch {
            classRepository.getAllClassGroups().collect { classes ->
                val newMemberships = mutableMapOf<String, List<String>>()
                classes.forEach { clazz ->
                    classRepository.getClassMembers(clazz.id).collect { members ->
                        newMemberships[clazz.id] = members.map { it.studentId }
                        _uiState.update { it.copy(classes = classes, classMemberships = newMemberships, isLoading = false) }
                    }
                }
                if (classes.isEmpty()) {
                    _uiState.update { it.copy(classes = classes, isLoading = false) }
                }
            }
        }
    }

    // ── Password change ───────────────────────────────────────────────────────

    fun showPasswordDialog()  { _uiState.update { it.copy(showPasswordDialog = true) } }
    fun dismissPasswordDialog() {
        _uiState.update { it.copy(showPasswordDialog = false, newPassword = "", confirmPassword = "", passwordChangeFeedback = null) }
    }
    fun onNewPasswordChange(v: String)     { _uiState.update { it.copy(newPassword = v, passwordChangeFeedback = null) } }
    fun onConfirmPasswordChange(v: String) { _uiState.update { it.copy(confirmPassword = v, passwordChangeFeedback = null) } }

    fun submitPasswordChange() {
        val state = _uiState.value
        if (state.newPassword.length < 6) {
            _uiState.update { it.copy(passwordChangeFeedback = "Password must be at least 6 characters.") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(passwordChangeFeedback = "Passwords do not match.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(passwordChangeLoading = true) }
            val result = authRepository.changeOwnPassword(state.newPassword)
            when (result) {
                is com.must.connect.data.model.AuthResult.Success -> {
                    _uiState.update { it.copy(passwordChangeLoading = false, showPasswordDialog = false, feedbackMessage = "Password changed successfully!") }
                }
                is com.must.connect.data.model.AuthResult.Error -> {
                    _uiState.update { it.copy(passwordChangeLoading = false, passwordChangeFeedback = result.message) }
                }
                else -> {}
            }
        }
    }

    // ── Create Class ──────────────────────────────────────────────────────────

    fun showCreateClassDialog()    { _uiState.update { it.copy(showCreateClassDialog = true) } }
    fun dismissCreateClassDialog() { _uiState.update { it.copy(showCreateClassDialog = false, newClassName = "", newSubject = "", newSection = "", newSemester = "") } }
    fun onNewClassNameChange(v: String)    { _uiState.update { it.copy(newClassName = v) } }
    fun onNewSubjectChange(v: String)      { _uiState.update { it.copy(newSubject = v) } }
    fun onNewSectionChange(v: String)      { _uiState.update { it.copy(newSection = v) } }
    fun onNewSemesterChange(v: String)     { _uiState.update { it.copy(newSemester = v) } }

    fun submitCreateClass() {
        val state   = _uiState.value
        val profile = state.profile ?: return
        if (state.newClassName.isBlank() || state.newSubject.isBlank() || state.newSection.isBlank() || state.newSemester.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "All fields required.") }
            return
        }
        val semInt = state.newSemester.toIntOrNull()
        if (semInt == null || semInt !in 1..8) {
            _uiState.update { it.copy(feedbackMessage = "Semester must be a number between 1 and 8.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(createClassLoading = true) }
            val result = classRepository.createClassGroup(
                name      = state.newClassName.trim(),
                subject   = state.newSubject.trim(),
                section   = state.newSection.trim(),
                semester  = semInt,
                teacherId = null,
                createdBy = profile.userId,
            )
            result.onSuccess {
                _uiState.update { it.copy(
                    createClassLoading = false,
                    showCreateClassDialog = false,
                    newClassName = "", newSubject = "", newSection = "", newSemester = "",
                    feedbackMessage = "Class created!"
                ) }
                loadAll()
            }.onFailure { e ->
                _uiState.update { it.copy(createClassLoading = false, feedbackMessage = e.message ?: "Failed to create class.") }
            }
        }
    }

    // ── Manage Class (Teacher & Students) ─────────────────────────────────────
    
    fun assignTeacherToClass(classId: String, teacherId: String) {
        viewModelScope.launch {
            classRepository.assignTeacherToClass(classId, teacherId).onSuccess {
                _uiState.update { it.copy(feedbackMessage = "Teacher assigned.") }
                loadAll()
            }.onFailure { e ->
                _uiState.update { it.copy(feedbackMessage = e.message) }
            }
        }
    }

    fun enrollStudentsToClass(classId: String, studentIds: List<String>) {
        if (studentIds.isEmpty()) return
        viewModelScope.launch {
            classRepository.addStudentsToClass(classId, studentIds).onSuccess {
                _uiState.update { it.copy(feedbackMessage = "Students enrolled.") }
                loadAll()
            }.onFailure { e ->
                _uiState.update { it.copy(feedbackMessage = e.message) }
            }
        }
    }

    // ── Admin Manage Users ──────────────────────────────────────────────────

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.deleteUser(userId).onSuccess {
                _uiState.update { it.copy(isLoading = false, feedbackMessage = "User deleted successfully.") }
                loadAll() // Refresh lists
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, feedbackMessage = e.message ?: "Failed to delete user.") }
            }
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            classRepository.deleteClass(classId).onSuccess {
                _uiState.update { it.copy(isLoading = false, feedbackMessage = "Class deleted successfully.") }
                loadAll()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, feedbackMessage = e.message ?: "Failed to delete class.") }
            }
        }
    }

    // ── Admin Password Reset ──────────────────────────────────────────────────
    
    fun resetUserPassword(userId: String) {
        // Just set to a default for testing, e.g. "Password@123"
        val tempPassword = "Password@123"
        viewModelScope.launch {
            authRepository.adminResetPassword(userId, tempPassword).collect { result ->
                when (result) {
                    is AuthResult.Success -> {
                        _uiState.update { it.copy(feedbackMessage = "Password reset to $tempPassword") }
                    }
                    is AuthResult.Error -> {
                        _uiState.update { it.copy(feedbackMessage = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearFeedback() { _uiState.update { it.copy(feedbackMessage = null) } }
}

