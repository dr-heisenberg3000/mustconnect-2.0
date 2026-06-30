package com.must.connect.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Role enum ─────────────────────────────────────────────────────────────────

/**
 * The four strictly-defined user roles from project_rules.md and
 * supabase_rules.md.  Each role maps to its own Supabase profile table.
 */
enum class UserRole(
    /** The Supabase table that holds this role's profile row. */
    val profileTable: String,
) {
    SUPER_ADMIN(profileTable = "super_admin_profiles"),
    DEPT_ADMIN  (profileTable = "dept_admin_profiles"),
    TEACHER     (profileTable = "teacher_profiles"),
    STUDENT     (profileTable = "student_profiles"),
}

// ── Profile data classes (one per table) ─────────────────────────────────────
// These @Serializable classes are decoded by PostgREST after a successful
// login.  Aligned to the ACTUAL Supabase schema columns.

interface UserProfile {
    val id: String
    val userId: String
    val fullName: String
    val email: String
    val isActive: Boolean
    val avatarUrl: String?
}

@Serializable
data class StudentProfile(
    @SerialName("id")          override val id         : String,
    @SerialName("user_id")     override val userId     : String,
    @SerialName("full_name")   override val fullName   : String,
    @SerialName("email")       override val email      : String  = "",
    @SerialName("roll_number") val rollNumber : String,
    @SerialName("section")     val section    : String  = "",
    @SerialName("semester")    val semester   : Int     = 0,
    @SerialName("is_active")   override val isActive   : Boolean = true,
    @SerialName("avatar_url")  override val avatarUrl  : String? = null,
    @SerialName("created_at")  val createdAt  : String  = "",
    @SerialName("updated_at")  val updatedAt  : String  = "",
) : UserProfile

@Serializable
data class TeacherProfile(
    @SerialName("id")          override val id          : String,
    @SerialName("user_id")     override val userId      : String,
    @SerialName("full_name")   override val fullName    : String,
    @SerialName("email")       override val email       : String  = "",
    @SerialName("username")    val username    : String,
    @SerialName("designation") val designation : String  = "",
    @SerialName("is_active")   override val isActive    : Boolean = true,
    @SerialName("avatar_url")  override val avatarUrl   : String? = null,
    @SerialName("created_at")  val createdAt   : String  = "",
    @SerialName("updated_at")  val updatedAt   : String  = "",
) : UserProfile

@Serializable
data class DeptAdminProfile(
    @SerialName("id")          override val id        : String,
    @SerialName("user_id")     override val userId    : String,
    @SerialName("full_name")   override val fullName  : String,
    @SerialName("email")       override val email     : String  = "",
    @SerialName("username")    val username  : String,
    @SerialName("is_active")   override val isActive  : Boolean = true,
    @SerialName("avatar_url")  override val avatarUrl : String? = null,
    @SerialName("created_at")  val createdAt : String  = "",
    @SerialName("updated_at")  val updatedAt : String  = "",
) : UserProfile

@Serializable
data class SuperAdminProfile(
    @SerialName("id")          override val id        : String,
    @SerialName("user_id")     override val userId    : String,
    @SerialName("full_name")   override val fullName  : String,
    @SerialName("email")       override val email     : String  = "",
    @SerialName("username")    val username  : String,
    @SerialName("is_active")   override val isActive  : Boolean = true,
    @SerialName("avatar_url")  override val avatarUrl : String? = null,
    @SerialName("created_at")  val createdAt : String  = "",
    @SerialName("updated_at")  val updatedAt : String  = "",
) : UserProfile

// ── Sealed result wrapper ─────────────────────────────────────────────────────

/**
 * Generic three-state Result used across all repository calls so the UI layer
 * never has to deal with raw exceptions.
 */
sealed class AuthResult<out T> {
    data object Loading : AuthResult<Nothing>()
    data class  Success<T>(val data: T) : AuthResult<T>()
    data class  Error(val message: String, val cause: Throwable? = null) : AuthResult<Nothing>()
}

/**
 * Convenience wrapper produced after a successful sign-in.  Carries both the
 * authenticated user's ID and the decoded role-specific profile.
 */
data class AuthenticatedUser(
    val userId : String,
    val role   : UserRole,
    val profile: UserProfile,   // concrete type is StudentProfile / TeacherProfile / …
)
