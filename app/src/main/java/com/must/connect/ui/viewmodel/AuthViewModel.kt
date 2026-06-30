package com.must.connect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.connect.data.model.AuthResult
import com.must.connect.data.model.AuthenticatedUser
import com.must.connect.data.model.UserRole
import com.must.connect.data.repository.AuthRepository
import com.must.connect.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

/**
 * The complete UI state for any login / sign-up screen.
 *
 *  - [Idle]    → empty form, no indicators
 *  - [Loading] → inputs disabled, spinner visible
 *  - [Success] → triggers navigation (consumed via [onNavigationConsumed])
 *  - [Error]   → inline banner, form re-enabled
 */
sealed class LoginUiState {
    data object Idle    : LoginUiState()
    data object Loading : LoginUiState()
    data class  Success(val user: AuthenticatedUser) : LoginUiState()
    data class  Error(val message: String) : LoginUiState()
}

// ── Form state ────────────────────────────────────────────────────────────────

/**
 * Holds live field values.  Kept separate from [LoginUiState] so typing
 * never resets a spinner or error banner.
 */
data class LoginFormState(
    val identifier         : String  = "",   // roll number (Student) or username
    val password           : String  = "",
    val isPasswordVisible  : Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * AuthViewModel
 *
 * Shared Activity-scoped instance across all login screens.
 * Delegates all I/O to [AuthRepository] (singleton) and exposes results
 * as [StateFlow]. Never holds a NavController reference — navigation is
 * triggered by the Compose layer reacting to [LoginUiState.Success].
 */
class AuthViewModel : ViewModel() {

    // Always use the singleton so authState is shared with AddUserViewModel
    private val repository: AuthRepository = AuthRepository.getInstance()

    // ── Exposed state ──────────────────────────────────────────────────────────

    private val _uiState   = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    // ── Form event handlers ────────────────────────────────────────────────────

    fun onIdentifierChange(value: String) {
        _formState.update { it.copy(identifier = value) }
        clearErrorIfPresent()
    }

    fun onPasswordChange(value: String) {
        _formState.update { it.copy(password = value) }
        clearErrorIfPresent()
    }

    fun onTogglePasswordVisibility() {
        _formState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    // ── Sign-in ────────────────────────────────────────────────────────────────

    /**
     * Validates the form, then delegates to [AuthRepository.signIn].
     * Emits [LoginUiState.Success] carrying the [AuthenticatedUser] on success.
     */
    fun onLoginClicked(role: UserRole) {
        val form = _formState.value
        if (!validateForm(role, form)) return

        viewModelScope.launch {
            repository
                .signIn(
                    role       = role,
                    identifier = form.identifier.trim(),
                    password   = form.password,
                )
                .collect { result -> _uiState.value = result.toLoginUiState() }
        }
    }

    // ── Sign-out ───────────────────────────────────────────────────────────────

    fun onSignOutClicked() {
        viewModelScope.launch {
            repository.signOut()
            resetState()
        }
    }

    // ── Change own password ────────────────────────────────────────────────────

    fun changePassword(
        newPassword : String,
        onSuccess   : () -> Unit,
        onError     : (String) -> Unit,
    ) {
        viewModelScope.launch {
            val result = repository.changeOwnPassword(newPassword)
            when (result) {
                is AuthResult.Success -> onSuccess()
                is AuthResult.Error   -> onError(result.message)
                else -> {}
            }
        }
    }

    // ── Navigation helpers ─────────────────────────────────────────────────────

    /**
     * Returns the correct dashboard route for a given [role].
     */
    fun dashboardRouteFor(role: UserRole): String = when (role) {
        UserRole.STUDENT     -> Screen.StudentDashboard.route
        UserRole.TEACHER     -> Screen.TeacherDashboard.route
        UserRole.DEPT_ADMIN  -> Screen.DeptAdminDashboard.route
        UserRole.SUPER_ADMIN -> Screen.SuperAdminDashboard.route
    }

    /**
     * Must be called immediately after reacting to [LoginUiState.Success]
     * to prevent re-navigation on subsequent recompositions.
     */
    fun onNavigationConsumed() {
        _uiState.value   = LoginUiState.Idle
        _formState.value = LoginFormState()
    }

    // ── Session check ──────────────────────────────────────────────────────────

    /** Returns true if a valid local Supabase session exists. */
    fun hasActiveSession(): Boolean = repository.isSessionActive()

    /** The currently logged-in user, if any. */
    fun getCurrentUser(): AuthenticatedUser? = repository.currentUser

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun validateForm(role: UserRole, form: LoginFormState): Boolean {
        val identifierLabel = if (role == UserRole.STUDENT) "roll number" else "username"

        return when {
            form.identifier.isBlank() -> {
                _uiState.value = LoginUiState.Error("Please enter your $identifierLabel.")
                false
            }
            form.password.isBlank() -> {
                _uiState.value = LoginUiState.Error("Please enter your password.")
                false
            }
            form.password.length < 6 -> {
                _uiState.value = LoginUiState.Error("Password must be at least 6 characters.")
                false
            }
            else -> true
        }
    }

    private fun clearErrorIfPresent() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }

    private fun resetState() {
        _uiState.value   = LoginUiState.Idle
        _formState.value = LoginFormState()
    }

    // ── AuthResult → LoginUiState mapping ─────────────────────────────────────

    private fun AuthResult<AuthenticatedUser>.toLoginUiState(): LoginUiState = when (this) {
        is AuthResult.Loading      -> LoginUiState.Loading
        is AuthResult.Success      -> LoginUiState.Success(data)
        is AuthResult.Error        -> LoginUiState.Error(message)
    }
}
