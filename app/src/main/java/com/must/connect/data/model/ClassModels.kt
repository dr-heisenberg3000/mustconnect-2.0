package com.must.connect.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `class_groups` Supabase table.
 * Actual DB columns: id, name, subject, section, semester, teacher_id, created_by, is_active, created_at, updated_at
 */
@Serializable
data class ClassGroup(
    @SerialName("id")          val id         : String  = "",
    @SerialName("name")        val name       : String,
    @SerialName("subject")     val subject    : String,
    @SerialName("section")     val section    : String  = "",
    @SerialName("semester")    val semester   : Int     = 0,
    @SerialName("teacher_id")  val teacherId  : String? = null,
    @SerialName("created_by")  val createdBy  : String,
    @SerialName("is_active")   val isActive   : Boolean = true,
    @SerialName("created_at")  val createdAt  : String  = "",
    @SerialName("updated_at")  val updatedAt  : String  = "",
)

/**
 * Maps to the `class_posts` Supabase table.
 * Actual DB columns: id, class_id, author_id, title, body, post_type, attachment_url, attachment_type, created_at, updated_at
 */
@Serializable
data class ClassPost(
    @SerialName("id")               val id             : String  = "",
    @SerialName("class_id")         val classId        : String,
    @SerialName("author_id")        val authorId       : String,
    @SerialName("title")            val title          : String,
    @SerialName("body")             val body           : String,
    @SerialName("post_type")        val postType       : String  = "ANNOUNCEMENT",
    @SerialName("attachment_url")   val attachmentUrl  : String? = null,
    @SerialName("attachment_type")  val attachmentType : String? = null,
    @SerialName("created_at")       val createdAt      : String  = "",
    @SerialName("updated_at")       val updatedAt      : String  = "",
)

/**
 * Maps to the `class_memberships` Supabase table.
 */
@Serializable
data class ClassMembership(
    @SerialName("id")         val id        : String = "",
    @SerialName("class_id")   val classId   : String,
    @SerialName("student_id") val studentId : String,
    @SerialName("joined_at")  val joinedAt  : String = "",
)

/**
 * Maps to the `direct_messages` Supabase table.
 * Actual DB columns: id, sender_id, receiver_id, body, is_read, created_at
 */
@Serializable
data class DirectMessage(
    @SerialName("id")          val id         : String  = "",
    @SerialName("sender_id")   val senderId   : String,
    @SerialName("receiver_id") val receiverId : String,
    @SerialName("body")        val body       : String,
    @SerialName("is_read")     val isRead     : Boolean = false,
    @SerialName("created_at")  val createdAt  : String  = "",
)

/**
 * A conversation summary (for listing conversations in the messaging hub).
 */
data class ConversationSummary(
    val otherUserId   : String,
    val otherUserName : String,
    val otherUserRole : String,
    val lastMessage   : String,
    val lastMessageAt : String,
)

/**
 * Maps to the `class_messages` Supabase table for WhatsApp-like group messaging.
 */
@Serializable
data class ClassMessage(
    @SerialName("id")         val id        : String = "",
    @SerialName("class_id")   val classId   : String,
    @SerialName("sender_id")  val senderId  : String,
    @SerialName("body")       val body      : String,
    @SerialName("created_at") val createdAt : String = "",
)
