package com.must.connect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.connect.data.model.AuthResult
import com.must.connect.data.model.UserRole
import com.must.connect.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddUserFormState(
    val identifier      : String   = "",   // roll number (student) or username (others)
    val password        : String   = "",
    val fullName        : String   = "",
    val designation     : String   = "",   // for teachers
    val section         : String   = "",   // for students
    val semester        : String   = "",   // for students
    val role            : UserRole = UserRole.STUDENT,
    val isLoading       : Boolean  = false,
    val feedbackMessage : String?  = null,
    val isError         : Boolean  = false
)

class AddUserViewModel : ViewModel() {

    // Use the singleton AuthRepository so authState is populated from the login
    private val authRepository: AuthRepository = AuthRepository.getInstance()

    private val _uiState = MutableStateFlow(AddUserFormState())
    val uiState: StateFlow<AddUserFormState> = _uiState.asStateFlow()

    val currentUserRole: UserRole?
        get() = authRepository.currentUser?.role

    fun onIdentifierChange(value: String) {
        _uiState.update { it.copy(identifier = value, feedbackMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, feedbackMessage = null) }
    }

    fun onFullNameChange(fullName: String) {
        _uiState.update { it.copy(fullName = fullName, feedbackMessage = null) }
    }

    fun onDesignationChange(designation: String) {
        _uiState.update { it.copy(designation = designation, feedbackMessage = null) }
    }

    fun onSectionChange(section: String) {
        _uiState.update { it.copy(section = section, feedbackMessage = null) }
    }

    fun onSemesterChange(semester: String) {
        _uiState.update { it.copy(semester = semester, feedbackMessage = null) }
    }

    fun onRoleChange(role: UserRole) {
        _uiState.update { it.copy(role = role, feedbackMessage = null, identifier = "") }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun onCreateUserClicked() {
        val state = _uiState.value

        // Validation
        if (state.identifier.isBlank() || state.password.isBlank() || state.fullName.isBlank()) {
            _uiState.update {
                it.copy(feedbackMessage = "Please fill in all required fields.", isError = true)
            }
            return
        }

        if (state.password.length < 6) {
            _uiState.update {
                it.copy(feedbackMessage = "Password must be at least 6 characters.", isError = true)
            }
            return
        }

        // Derive the system email from role + identifier
        val email = authRepository.buildEmail(state.role, state.identifier)

        // Build role-specific metadata for the Supabase trigger + Edge Function
        val displayRoleString = state.role.name

        val userMetadata: MutableMap<String, Any> = mutableMapOf(
            "role"      to displayRoleString,
            "full_name" to state.fullName.trim(),
        )

        when (state.role) {
            UserRole.STUDENT -> {
                userMetadata["roll_number"] = state.identifier.trim()
                if (state.section.isNotBlank()) userMetadata["section"] = state.section.trim()
                val sem = state.semester.toIntOrNull()
                if (sem != null) userMetadata["semester"] = sem
            }
            UserRole.TEACHER -> {
                userMetadata["username"] = state.identifier.trim()
                if (state.designation.isNotBlank()) userMetadata["designation"] = state.designation.trim()
            }
            UserRole.DEPT_ADMIN -> {
                userMetadata["username"] = state.identifier.trim()
            }
            UserRole.SUPER_ADMIN -> {
                userMetadata["username"] = state.identifier.trim()
            }
        }

        viewModelScope.launch {
            authRepository.adminCreateUser(
                role         = state.role,
                email        = email,
                password     = state.password,
                userMetadata = userMetadata
            ).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, feedbackMessage = null) }
                    }
                    is AuthResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading       = false,
                                feedbackMessage = "User '${state.fullName}' created successfully!",
                                isError         = false,
                                identifier      = "",
                                password        = "",
                                fullName        = "",
                                designation     = "",
                                section         = "",
                                semester        = "",
                            )
                        }
                    }
                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading       = false,
                                feedbackMessage = result.message,
                                isError         = true
                            )
                        }
                    }
                }
            }
        }
    }
}
