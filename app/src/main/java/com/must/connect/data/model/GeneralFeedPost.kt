package com.must.connect.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `general_feed_posts` Supabase table.
 * Only the Department Admin can create posts; Super Admin can delete them.
 * Teachers and Students have read-only access (enforced via RLS).
 *
 * Actual DB columns: id, author_id, title, body, pinned, attachment_url, attachment_type, created_at, updated_at
 */
@Serializable
data class GeneralFeedPost(
    @SerialName("id")               val id             : String  = "",
    @SerialName("title")            val title          : String,
    @SerialName("body")             val body           : String,
    @SerialName("author_id")        val authorId       : String,
    @SerialName("pinned")           val pinned         : Boolean = false,
    @SerialName("attachment_url")   val attachmentUrl  : String? = null,
    @SerialName("attachment_type")  val attachmentType : String? = null,
    @SerialName("created_at")       val createdAt      : String  = "",
    @SerialName("updated_at")       val updatedAt      : String  = "",
)
