package com.must.connect.data.repository

import com.must.connect.data.model.DeptAdminProfile
import com.must.connect.data.model.StudentProfile
import com.must.connect.data.model.SuperAdminProfile
import com.must.connect.data.model.TeacherProfile
import com.must.connect.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * UserRepository handles queries to the four profile tables.
 */
class UserRepository {

    private val client = SupabaseClientProvider.client

    // ── Students ──────────────────────────────────────────────────────────────

    fun getAllStudents(): Flow<List<StudentProfile>> = flow {
        try {
            val result = client.from("student_profiles")
                .select { order("full_name", Order.ASCENDING) }
                .decodeList<StudentProfile>()
            emit(result)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun getStudentById(studentId: String): StudentProfile? {
        return try {
            client.from("student_profiles")
                .select { filter { eq("id", studentId) } }
                .decodeSingleOrNull<StudentProfile>()
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    suspend fun getStudentByUserId(userId: String): StudentProfile? {
        return try {
            client.from("student_profiles")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<StudentProfile>()
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    suspend fun getStudentsByUserIds(userIds: List<String>): List<StudentProfile> {
        if (userIds.isEmpty()) return emptyList()
        return try {
            client.from("student_profiles")
                .select { filter { isIn("user_id", userIds) } }
                .decodeList<StudentProfile>()
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    // ── Teachers ──────────────────────────────────────────────────────────────

    fun getAllTeachers(): Flow<List<TeacherProfile>> = flow {
        try {
            val result = client.from("teacher_profiles")
                .select { order("full_name", Order.ASCENDING) }
                .decodeList<TeacherProfile>()
            emit(result)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun getTeacherById(teacherId: String): TeacherProfile? {
        return try {
            client.from("teacher_profiles")
                .select { filter { eq("id", teacherId) } }
                .decodeSingleOrNull<TeacherProfile>()
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    suspend fun getTeacherByUserId(userId: String): TeacherProfile? {
        return try {
            client.from("teacher_profiles")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<TeacherProfile>()
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    suspend fun getTeachersByUserIds(userIds: List<String>): List<TeacherProfile> {
        if (userIds.isEmpty()) return emptyList()
        return try {
            client.from("teacher_profiles")
                .select { filter { isIn("user_id", userIds) } }
                .decodeList<TeacherProfile>()
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
    }

    // ── Dept Admin ────────────────────────────────────────────────────────────

    suspend fun getDeptAdminByUserId(userId: String): DeptAdminProfile? {
        return try {
            client.from("dept_admin_profiles")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<DeptAdminProfile>()
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    fun getAllDeptAdmins(): Flow<List<DeptAdminProfile>> = flow {
        try {
            val result = client.from("dept_admin_profiles")
                .select { order("full_name", Order.ASCENDING) }
                .decodeList<DeptAdminProfile>()
            emit(result)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            emit(emptyList())
        }
    }

    // ── Super Admin ───────────────────────────────────────────────────────────

    suspend fun getSuperAdminByUserId(userId: String): SuperAdminProfile? {
        return try {
            client.from("super_admin_profiles")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<SuperAdminProfile>()
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    // ── Management ────────────────────────────────────────────────────────────

    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            client.functions.invoke("admin-delete-user") {
                setBody(kotlinx.serialization.json.buildJsonObject {
                    put("targetUserId", kotlinx.serialization.json.JsonPrimitive(userId))
                })
            }
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }
}
