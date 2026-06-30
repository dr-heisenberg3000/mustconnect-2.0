package com.must.connect.data.repository

import com.must.connect.data.model.AuthResult
import com.must.connect.data.model.AuthenticatedUser
import com.must.connect.data.model.DeptAdminProfile
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.model.SuperAdminProfile
import com.must.connect.data.model.TeacherProfile
import com.must.connect.data.model.UserRole
import com.must.connect.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.storage.storage
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * AuthRepository — com.must.connect.data.repository
 *
 * Single source of truth for all Supabase Auth operations in MUST-CONNECT.
 *
 * SINGLETON: Use [AuthRepository.instance] everywhere so that [authState]
 * is shared across all ViewModels (AdminCreateUser, AuthViewModel, etc.)
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  ARCHITECTURE CONTRACT (supabase_rules.md)                  │
 * │  • Backend is exclusively Supabase (PostgreSQL + Auth).     │
 * │  • Four strict role tables: student_profiles,               │
 * │    teacher_profiles, dept_admin_profiles,                   │
 * │    super_admin_profiles.                                    │
 * │  • Emails are derived from roll numbers / usernames —       │
 * │    users never see the underlying address.                  │
 * │  • Roles are NEVER read from client-writable user_metadata; │
 * │    they are confirmed by the presence of a profile row in   │
 * │    the correct RLS-protected table.                         │
 * └─────────────────────────────────────────────────────────────┘
 */
class AuthRepository private constructor() {

    private val client = SupabaseClientProvider.client

    // ── Hot auth state (shared across all observers) ──────────────────────────

    private val _authState = MutableStateFlow<AuthResult<AuthenticatedUser>>(AuthResult.Loading)

    /**
     * Hot [StateFlow] always containing the last auth result.
     * Initialises as [AuthResult.Loading] and updates after every [signIn] call.
     */
    val authState: StateFlow<AuthResult<AuthenticatedUser>> = _authState.asStateFlow()

    /** Convenience accessor: the currently authenticated user, or null. */
    val currentUser: AuthenticatedUser?
        get() = (_authState.value as? AuthResult.Success)?.data

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        /** Returns the single shared [AuthRepository] instance. */
        fun getInstance(): AuthRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository().also { INSTANCE = it }
            }
    }

    // ── Email construction ─────────────────────────────────────────────────────

    /**
     * Derives the deterministic Supabase email for the given role + identifier.
     *
     * | Role         | Identifier   | Resulting email                          |
     * |--------------|--------------|------------------------------------------|
     * | Student      | Roll number  | `{roll}@student.must.edu.pk`             |
     * | Teacher      | Username     | `{username}@teacher.must.edu.pk`         |
     * | Dept Admin   | Username     | `{username}@deptadmin.must.edu.pk`       |
     * | Super Admin  | Username     | `{username}@superadmin.must.edu.pk`      |
     */
    fun buildEmail(role: UserRole, identifier: String): String = when (role) {
        UserRole.STUDENT     -> "${identifier.trim().lowercase()}@student.must.edu.pk"
        UserRole.TEACHER     -> "${identifier.trim().lowercase()}@teacher.must.edu.pk"
        UserRole.DEPT_ADMIN  -> "${identifier.trim().lowercase()}@deptadmin.must.edu.pk"
        UserRole.SUPER_ADMIN -> "${identifier.trim().lowercase()}@superadmin.must.edu.pk"
    }

    // ── Metadata → JsonObject conversion ──────────────────────────────────────

    /**
     * Converts a plain [Map]<String, Any> from the call-site into the
     * [JsonObject] required by the Supabase Kotlin SDK v3 `data` parameter.
     */
    private fun Map<String, Any>.toJsonObject(): JsonObject = buildJsonObject {
        forEach { (key, value) ->
            when (value) {
                is String  -> put(key, value)
                is Boolean -> put(key, value)
                is Int     -> put(key, value)
                is Long    -> put(key, value)
                is Double  -> put(key, value)
                is Float   -> put(key, value.toDouble())
                else       -> put(key, JsonPrimitive(value.toString()))
            }
        }
    }

    // ── Avatar Upload ─────────────────────────────────────────────────────────

    suspend fun uploadAvatar(bytes: ByteArray, fileName: String): Result<String> {
        return try {
            val bucket = client.storage.from("avatars")
            val uniqueName = "${java.util.UUID.randomUUID()}_$fileName"
            bucket.upload(uniqueName, bytes) { upsert = true }
            Result.success(bucket.publicUrl(uniqueName))
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun updateProfileAvatar(userId: String, role: UserRole, avatarUrl: String): Result<Unit> {
        return try {
            client.from(role.profileTable)
                .update({
                    set("avatar_url", avatarUrl)
                }) {
                    filter { eq("user_id", userId) }
                }
            
            // Re-fetch user profile to update authState
            val currentAuth = _authState.value
            if (currentAuth is AuthResult.Success) {
                // To force a refresh of the current profile, we can re-decode the profile row.
                val updatedProfile = when (role) {
                    UserRole.STUDENT -> client.from(role.profileTable).select { filter { eq("user_id", userId) } }.decodeSingle<StudentProfile>()
                    UserRole.TEACHER -> client.from(role.profileTable).select { filter { eq("user_id", userId) } }.decodeSingle<TeacherProfile>()
                    UserRole.DEPT_ADMIN -> client.from(role.profileTable).select { filter { eq("user_id", userId) } }.decodeSingle<DeptAdminProfile>()
                    UserRole.SUPER_ADMIN -> client.from(role.profileTable).select { filter { eq("user_id", userId) } }.decodeSingle<SuperAdminProfile>()
                }
                _authState.value = AuthResult.Success(
                    currentAuth.data.copy(profile = updatedProfile)
                )
            }
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    // ── adminCreateUser ───────────────────────────────────────────────────────

    /**
     * Registers a new user account via the secure Edge Function.
     * Requires an active Dept Admin or Super Admin session in [authState].
     */
    fun adminCreateUser(
        role        : UserRole,
        email       : String,
        password    : String,
        userMetadata: Map<String, Any>,
    ): Flow<AuthResult<Unit>> = flow {

        emit(AuthResult.Loading)

        try {
            // ── Authorization Check ───────────────────────────────────────────
            val currentAuth = _authState.value
            if (currentAuth !is AuthResult.Success) {
                emit(AuthResult.Error("Unauthorized: No active admin session."))
                return@flow
            }

            val currentRole = currentAuth.data.role
            if (currentRole != UserRole.DEPT_ADMIN && currentRole != UserRole.SUPER_ADMIN) {
                emit(AuthResult.Error("Unauthorized: Only Department or Super Admins can create users."))
                return@flow
            }

            // ── Invoke Secure Edge Function ───────────────────────────────────
            client.functions.invoke("create-admin-user") {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("email", email)
                    put("password", password)
                    put("userMetadata", userMetadata.toJsonObject())
                })
            }

            emit(AuthResult.Success(Unit))

        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            emit(AuthResult.Error(
                message = mapExceptionToMessage(e, role, isSignUp = true),
                cause = e
            ))
        }
    }

    // ── resetUserPassword ─────────────────────────────────────────────────────

    /**
     * Resets a user's password via the admin Edge Function.
     * Requires Dept Admin or Super Admin session.
     */
    fun resetUserPassword(
        userEmail   : String,
        newPassword : String,
    ): Flow<AuthResult<Unit>> = flow {
        emit(AuthResult.Loading)
        try {
            val currentAuth = _authState.value
            if (currentAuth !is AuthResult.Success) {
                emit(AuthResult.Error("Unauthorized: No active admin session."))
                return@flow
            }
            client.functions.invoke("create-admin-user") {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("action", "reset_password")
                    put("email", userEmail)
                    put("password", newPassword)
                })
            }
            emit(AuthResult.Success(Unit))
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            emit(AuthResult.Error(e.message ?: "Failed to invoke edge function."))
        }
    }

    // ── adminResetPassword ────────────────────────────────────────────────────

    /**
     * Resets a user's password via the secure Edge Function.
     * Requires an active Dept Admin or Super Admin session.
     */
    fun adminResetPassword(targetUserId: String, newPassword: String): Flow<AuthResult<Unit>> = flow {
        emit(AuthResult.Loading)
        try {
            val currentAuth = _authState.value
            if (currentAuth !is AuthResult.Success) {
                emit(AuthResult.Error("Unauthorized: No active admin session."))
                return@flow
            }
            val currentRole = currentAuth.data.role
            if (currentRole != UserRole.DEPT_ADMIN && currentRole != UserRole.SUPER_ADMIN) {
                emit(AuthResult.Error("Unauthorized: Only Admins can reset passwords."))
                return@flow
            }

            val payload = buildJsonObject {
                put("targetUserId", targetUserId)
                put("newPassword", newPassword)
            }

            client.functions.invoke("admin-reset-password") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            emit(AuthResult.Success(Unit))

        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emit(AuthResult.Error(e.message ?: "Failed to reset password via edge function."))
        }
    }

    // ── changeOwnPassword ─────────────────────────────────────────────────────

    /**
     * Changes the current user's own password.
     */
    suspend fun changeOwnPassword(newPassword: String): AuthResult<Unit> {
        return try {
            client.auth.updateUser {
                password = newPassword
            }
            AuthResult.Success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            AuthResult.Error(e.message ?: "Failed to change password.", e)
        }
    }

    // ── signIn ────────────────────────────────────────────────────────────────

    /**
     * Authenticates an existing user and fetches their role-specific profile.
     */
    fun signIn(
        role      : UserRole,
        identifier: String,
        password  : String,
    ): Flow<AuthResult<AuthenticatedUser>> = flow {

        emit(AuthResult.Loading)
        _authState.value = AuthResult.Loading

        try {
            val email = buildEmail(role, identifier)

            // ── Step 1: Authenticate ──────────────────────────────────────────
            client.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }

            // ── Step 2: Fetch the role-specific profile ───────────────────────
            val result = fetchProfileForRole(role)
            _authState.value = result
            emit(result)

        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            runCatching { client.auth.signOut() }
            val error = AuthResult.Error(mapExceptionToMessage(e, role, isSignUp = false), e)
            _authState.value = error
            emit(error)
        }
    }

    // ── signOut ───────────────────────────────────────────────────────────────

    /**
     * Signs out the current user and resets [authState] to Loading (neutral).
     */
    suspend fun signOut(): AuthResult<Unit> {
        return try {
            client.auth.signOut()
            _authState.value = AuthResult.Loading
            AuthResult.Success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            AuthResult.Error(
                message = "Sign-out failed: ${e.message ?: "Unknown error."}",
                cause   = e,
            )
        }
    }

    // ── Session helpers ───────────────────────────────────────────────────────

    /** Returns `true` when Supabase reports a non-expired local session. */
    fun isSessionActive(): Boolean =
        client.auth.currentSessionOrNull() != null

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun fetchProfileForRole(role: UserRole): AuthResult<AuthenticatedUser> {
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException(
                "Auth succeeded but no session was created. " +
                "If email confirmation is enabled, the user must verify their " +
                "email before they can log in."
            )

        val authenticatedUser = when (role) {

            UserRole.STUDENT -> {
                val profile = client
                    .from(role.profileTable)
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingle<StudentProfile>()
                AuthenticatedUser(userId = userId, role = role, profile = profile)
            }

            UserRole.TEACHER -> {
                val profile = client
                    .from(role.profileTable)
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingle<TeacherProfile>()
                AuthenticatedUser(userId = userId, role = role, profile = profile)
            }

            UserRole.DEPT_ADMIN -> {
                val profile = client
                    .from(role.profileTable)
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingle<DeptAdminProfile>()
                AuthenticatedUser(userId = userId, role = role, profile = profile)
            }

            UserRole.SUPER_ADMIN -> {
                val profile = client
                    .from(role.profileTable)
                    .select { filter { eq("user_id", userId) } }
                    .decodeSingle<SuperAdminProfile>()
                AuthenticatedUser(userId = userId, role = role, profile = profile)
            }
        }

        return AuthResult.Success(authenticatedUser)
    }

    private fun mapExceptionToMessage(
        e        : Exception,
        role     : UserRole,
        isSignUp : Boolean,
    ): String {
        val identifierLabel = if (role == UserRole.STUDENT) "roll number" else "username"
        val msg = e.message ?: ""

        return when {
            msg.contains("Invalid login credentials", ignoreCase = true) ->
                "Incorrect $identifierLabel or password."

            msg.contains("User already registered", ignoreCase = true) ||
            msg.contains("already been registered", ignoreCase = true) ->
                "An account with this $identifierLabel already exists."

            msg.contains("Email not confirmed", ignoreCase = true) ->
                "Your account has not been activated yet. Please contact the department."

            msg.contains("Password should be", ignoreCase = true) ||
            msg.contains("should be at least", ignoreCase = true) ->
                "Password must be at least 6 characters."

            msg.contains("network", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ->
                "No internet connection. Please check your network and try again."

            msg.contains("no session", ignoreCase = true) ||
            msg.contains("session was created", ignoreCase = true) ->
                if (isSignUp)
                    "Account created! Check your email to confirm before logging in."
                else
                    "Session could not be established. Please try again."

            else -> if (isSignUp)
                "Registration failed: $msg"
            else
                "Login failed: $msg"
        }
    }
}
