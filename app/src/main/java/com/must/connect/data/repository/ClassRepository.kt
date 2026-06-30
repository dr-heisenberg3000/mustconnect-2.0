package com.must.connect.data.repository

import com.must.connect.data.model.ClassGroup
import com.must.connect.data.model.ClassMembership
import com.must.connect.data.model.ClassPost
import com.must.connect.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * ClassRepository handles all operations on class_groups, class_posts,
 * and class_memberships tables.
 */
class ClassRepository {

    private val client = SupabaseClientProvider.client

    // ── Class Groups ──────────────────────────────────────────────────────────

    /** All active class groups (all roles can read per RLS). */
    fun getAllClassGroups(): Flow<List<ClassGroup>> = flow {
        try {
            val result = client.from("class_groups")
                .select { filter { eq("is_active", true) }; order("created_at", Order.DESCENDING) }
                .decodeList<ClassGroup>()
            emit(result)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emit(emptyList())
        }
    }

    /** Class groups assigned to a specific teacher (by teacher profile id). */
    fun getClassGroupsForTeacher(teacherId: String): Flow<List<ClassGroup>> = flow {
        try {
            val result = client.from("class_groups")
                .select { filter { eq("teacher_id", teacherId); eq("is_active", true) }; order("created_at", Order.DESCENDING) }
                .decodeList<ClassGroup>()
            emit(result)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emit(emptyList())
        }
    }

    /** Class groups a student is enrolled in. */
    fun getClassGroupsForStudent(studentId: String): Flow<List<ClassGroup>> = flow {
        try {
            // Get membership IDs first
            val memberships = client.from("class_memberships")
                .select { filter { eq("student_id", studentId) } }
                .decodeList<ClassMembership>()
            val classIds = memberships.map { it.classId }
            if (classIds.isEmpty()) { emit(emptyList()); return@flow }
            // Fetch ALL active groups and filter client-side for enrolled ones
            val allGroups = client.from("class_groups")
                .select { filter { eq("is_active", true) } }
                .decodeList<ClassGroup>()
            val groups = allGroups.filter { it.id in classIds }
            emit(groups)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emit(emptyList())
        }
    }

    /** Create a new class group. Only dept admin can do this (RLS). */
    suspend fun createClassGroup(
        name      : String,
        subject   : String,
        section   : String,
        semester  : Int,
        teacherId : String?,
        createdBy : String,
    ): Result<Unit> {
        return try {
            val group = ClassGroup(
                name      = name,
                subject   = subject,
                section   = section,
                semester  = semester,
                teacherId = teacherId,
                createdBy = createdBy,
            )
            client.from("class_groups").insert(group)
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Assign a teacher to a class group. */
    suspend fun assignTeacherToClass(classId: String, teacherId: String): Result<Unit> {
        return try {
            client.from("class_groups")
                .update({ set("teacher_id", teacherId) }) {
                    filter { eq("id", classId) }
                }
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Add a student to a class group. */
    suspend fun addStudentToClass(classId: String, studentId: String): Result<Unit> {
        return try {
            val membership = ClassMembership(classId = classId, studentId = studentId)
            client.from("class_memberships").insert(membership)
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Add multiple students to a class group. */
    suspend fun addStudentsToClass(classId: String, studentIds: List<String>): Result<Unit> {
        return try {
            val memberships = studentIds.map { ClassMembership(classId = classId, studentId = it) }
            client.from("class_memberships").insert(memberships)
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Get all members (student ids) of a class. */
    fun getClassMembers(classId: String): Flow<List<ClassMembership>> = flow {
        try {
            val result = client.from("class_memberships")
                .select { filter { eq("class_id", classId) } }
                .decodeList<ClassMembership>()
            emit(result)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            emit(emptyList())
        }
    }

    // ── Class Posts ───────────────────────────────────────────────────────────

    /**
     * Get posts for a specific class, newest first.
     * Polls every [pollIntervalMs] for auto-refresh.
     */
    fun getClassPosts(classId: String, pollIntervalMs: Long = 10_000L): Flow<List<ClassPost>> = flow {
        while (true) {
            try {
                val result = client.from("class_posts")
                    .select { filter { eq("class_id", classId) }; order("created_at", Order.DESCENDING) }
                    .decodeList<ClassPost>()
                emit(result)
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
                emit(emptyList())
            }
            delay(pollIntervalMs)
        }
    }

    /** Create a new class post (teachers only, enforced by RLS). */
    suspend fun createClassPost(
        classId  : String,
        authorId : String,
        title    : String,
        body     : String,
        postType : String = "ANNOUNCEMENT",
        attachmentBytes: ByteArray? = null,
        attachmentName : String? = null,
        attachmentType : String? = null
    ): Result<Unit> {
        return try {
            var finalUrl: String? = null

            if (attachmentBytes != null && attachmentName != null) {
                val bucket = client.storage.from("class_media")
                val uniqueName = "${java.util.UUID.randomUUID()}_$attachmentName"
                bucket.upload(uniqueName, attachmentBytes) { upsert = false }
                finalUrl = bucket.publicUrl(uniqueName)
            }

            val post = ClassPost(
                classId  = classId,
                authorId = authorId,
                title    = title,
                body     = body,
                postType = postType,
                attachmentUrl = finalUrl,
                attachmentType = attachmentType
            )
            client.from("class_posts").insert(post)
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Delete a class post (own post or admin, enforced by RLS). */
    suspend fun deleteClassPost(postId: String): Result<Unit> {
        return try {
            client.from("class_posts").delete { filter { eq("id", postId) } }
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }
}
