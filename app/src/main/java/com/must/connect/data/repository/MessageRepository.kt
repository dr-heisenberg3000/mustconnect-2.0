package com.must.connect.data.repository

import com.must.connect.data.model.DirectMessage
import com.must.connect.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * MessageRepository handles all operations on the `direct_messages` table.
 * Real schema: id, sender_id, receiver_id, body, is_read, created_at
 */
class MessageRepository {

    private val client = SupabaseClientProvider.client

    /** Get the full conversation between two users. */
    fun getConversation(userId: String, otherUserId: String): Flow<List<DirectMessage>> = flow {
        try {
            // Fetch messages where user is sender or receiver, ordered by time
            val result = client.from("direct_messages")
                .select {
                    filter {
                        isIn("sender_id", listOf(userId, otherUserId))
                        isIn("receiver_id", listOf(userId, otherUserId))
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<DirectMessage>()
            
            // Filter further in-memory just to be strictly sure it's between these two
            val filtered = result.filter { 
                (it.senderId == userId && it.receiverId == otherUserId) || 
                (it.senderId == otherUserId && it.receiverId == userId) 
            }
            emit(filtered)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emit(emptyList())
        }
    }

    /** Get all distinct conversation partners for a user. */
    fun getConversationPartners(userId: String): Flow<List<DirectMessage>> = flow {
        try {
            // Get all messages involving this user
            val sentMessages = client.from("direct_messages")
                .select { filter { eq("sender_id", userId) }; order("created_at", Order.DESCENDING) }
                .decodeList<DirectMessage>()
            val receivedMessages = client.from("direct_messages")
                .select { filter { eq("receiver_id", userId) }; order("created_at", Order.DESCENDING) }
                .decodeList<DirectMessage>()
            // Combine and emit (UI will deduplicate by partner)
            val combined = (sentMessages + receivedMessages)
                .sortedByDescending { it.createdAt }
            emit(combined)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emit(emptyList())
        }
    }

    /** Send a new direct message. */
    suspend fun sendMessage(
        senderId   : String,
        receiverId : String,
        body       : String,
    ): Result<Unit> {
        return try {
            val message = DirectMessage(
                senderId   = senderId,
                receiverId = receiverId,
                body       = body,
            )
            client.from("direct_messages").insert(message)
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /** Mark messages as read. */
    suspend fun markMessagesRead(userId: String, senderId: String): Result<Unit> {
        return try {
            client.from("direct_messages")
                .update({ set("is_read", true) }) {
                    filter {
                        eq("receiver_id", userId)
                        eq("sender_id", senderId)
                        eq("is_read", false)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    // ── Class Group Messaging ─────────────────────────────────────────────────

    /** Get messages for a class group */
    fun getClassMessages(classId: String): Flow<List<com.must.connect.data.model.ClassMessage>> = flow {
        try {
            val result = client.from("class_messages")
                .select {
                    filter { eq("class_id", classId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<com.must.connect.data.model.ClassMessage>()
            emit(result)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            emit(emptyList())
        }
    }

    /** Send a class message */
    suspend fun sendClassMessage(classId: String, senderId: String, body: String): Result<Unit> {
        return try {
            val message = com.must.connect.data.model.ClassMessage(
                classId = classId,
                senderId = senderId,
                body = body
            )
            client.from("class_messages").insert(message)
            Result.success(Unit)
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }
}
