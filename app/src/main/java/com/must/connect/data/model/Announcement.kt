package com.must.connect.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Announcement(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("type") val type: String = "GENERAL", // GENERAL or CLASS
    @SerialName("class_id") val classId: String? = null
)
