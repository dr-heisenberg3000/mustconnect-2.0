package com.must.connect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.connect.data.model.AuthResult
import com.must.connect.data.model.ClassGroup
import com.must.connect.data.model.DeptAdminProfile
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.model.SuperAdminProfile
import com.must.connect.data.model.TeacherProfile
import com.must.connect.data.repository.AuthRepository
import com.must.connect.data.repository.ClassRepository
import com.must.connect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SuperAdminUiState(
    val profile         : SuperAdminProfile?   = null,
    val students        : List<StudentProfile>  = emptyList(),
    val teachers        : List<TeacherProfile>  = emptyList(),
    val deptAdmins      : List<DeptAdminProfile> = emptyList(),
    val classes         : List<ClassGroup>      = emptyList(),
    val isLoading       : Boolean               = false,
    val feedbackMessage : String?               = null,
    val isError         : Boolean               = false,
    // Password dialogs
    val showPasswordDialog     : Boolean = false,
    val newPassword            : String  = "",
    val confirmPassword        : String  = "",
    val passwordChangeLoading  : Boolean = false,
    val passwordChangeFeedback : String? = null,
)

class SuperAdminViewModel : ViewModel() {

    private val authRepository = AuthRepository.getInstance()
    private val userRepository = UserRepository()
    private val classRepository = ClassRepository()

    private val _uiState = MutableStateFlow(SuperAdminUiState())
    val uiState: StateFlow<SuperAdminUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        val user    = authRepository.currentUser ?: return
        val profile = user.profile as? SuperAdminProfile
        _uiState.update { it.copy(profile = profile) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
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
            userRepository.getAllDeptAdmins().collect { admins ->
                _uiState.update { it.copy(deptAdmins = admins) }
            }
        }
        viewModelScope.launch {
            classRepository.getAllClassGroups().collect { classes ->
                _uiState.update { it.copy(classes = classes, isLoading = false) }
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
                is AuthResult.Success -> {
                    _uiState.update { it.copy(passwordChangeLoading = false, showPasswordDialog = false, feedbackMessage = "Password changed!") }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(passwordChangeLoading = false, passwordChangeFeedback = result.message) }
                }
                else -> {}
            }
        }
    }

    fun clearFeedback() { _uiState.update { it.copy(feedbackMessage = null) } }
}

