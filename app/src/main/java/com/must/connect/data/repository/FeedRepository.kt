package com.must.connect.data.repository

import com.must.connect.data.model.GeneralFeedPost
import com.must.connect.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.github.jan.supabase.storage.storage

/**
 * FeedRepository handles all operations on the `general_feed_posts` table.
 *
 * Real schema: id, author_id, title, body, pinned, attachment_url,
 *              attachment_type, created_at, updated_at
 */
class FeedRepository {

    private val client = SupabaseClientProvider.client

    /**
     * Fetches all general feed posts ordered by pinned first, then newest.
     * Polls every [pollIntervalMs] milliseconds so the UI auto-refreshes
     * without requiring a Supabase Realtime subscription.
     */
    fun getGeneralFeedPosts(pollIntervalMs: Long = 15_000L): Flow<List<GeneralFeedPost>> = flow {
        while (true) {
            try {
                val result = client.from("general_feed_posts")
                    .select {
                        order("pinned", Order.DESCENDING)
                        order("created_at", Order.DESCENDING)
                    }.decodeList<GeneralFeedPost>()
                emit(result)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
                emit(emptyList())
            }
            delay(pollIntervalMs)
        }
    }

    /**
     * Creates a new general feed post.
     * Only the Department Admin is permitted by RLS.
     */
    suspend fun createPost(
        title          : String,
        body           : String,
        authorId       : String,
        pinned         : Boolean,
        attachmentBytes: ByteArray? = null,
        attachmentName : String? = null,
        attachmentType : String? = null
    ): Result<Unit> {
        return try {
            var finalUrl: String? = null

            if (attachmentBytes != null && attachmentName != null) {
                val bucket = client.storage.from("announcements")
                val uniqueName = "${java.util.UUID.randomUUID()}_$attachmentName"
                bucket.upload(uniqueName, attachmentBytes) { upsert = false }
                finalUrl = bucket.publicUrl(uniqueName)
            }

            val post = GeneralFeedPost(
                title          = title,
                body           = body,
                authorId       = authorId,
                pinned         = pinned,
                attachmentUrl  = finalUrl,
                attachmentType = attachmentType
            )
            client.from("general_feed_posts").insert(post)
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /**
     * Deletes a general feed post by ID.
     * RLS allows dept admin (own posts) and super admin (any post).
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            client.from("general_feed_posts")
                .delete { filter { eq("id", postId) } }
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }
}
