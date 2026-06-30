package com.must.connect.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.connect.data.model.GeneralFeedPost
import com.must.connect.data.remote.SupabaseClientProvider
import com.must.connect.data.repository.AuthRepository
import com.must.connect.data.repository.FeedRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedState(
    val posts      : List<GeneralFeedPost> = emptyList(),
    val authors    : Map<String, com.must.connect.data.model.UserProfile> = emptyMap(),
    val isLoading  : Boolean               = false,
    val error      : String?               = null,
)

class FeedViewModel(
    private val repository    : FeedRepository = FeedRepository(),
    private val authRepository: AuthRepository  = AuthRepository.getInstance(),
    private val userRepository: com.must.connect.data.repository.UserRepository = com.must.connect.data.repository.UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedState())
    val uiState: StateFlow<FeedState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getGeneralFeedPosts().collect { posts ->
                val authorIds = posts.map { it.authorId }.distinct()
                val newAuthors = _uiState.value.authors.toMutableMap()
                authorIds.forEach { aid ->
                    if (!newAuthors.containsKey(aid)) {
                        val profile = userRepository.getDeptAdminByUserId(aid)
                            ?: userRepository.getSuperAdminByUserId(aid)
                            ?: userRepository.getTeacherByUserId(aid)
                            ?: userRepository.getStudentByUserId(aid)
                        if (profile != null) newAuthors[aid] = profile
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    posts     = posts,
                    authors   = newAuthors
                )
            }
        }
    }

    /**
     * Creates a new general feed post.
     * author_name is NOT stored in the DB table — it is derived from the
     * current user's session metadata for display purposes only.
     */
    fun createPost(
        title          : String,
        body           : String,
        pinned         : Boolean,
        attachmentBytes: ByteArray? = null,
        attachmentName : String? = null,
        attachmentType : String? = null,
        onSuccess      : () -> Unit,
        onError        : (String) -> Unit
    ) {
        viewModelScope.launch {
            val session = SupabaseClientProvider.client.auth.currentSessionOrNull()
            val user = session?.user
            if (user == null) {
                onError("User not logged in")
                return@launch
            }

            val result = repository.createPost(
                title          = title,
                body           = body,
                authorId       = user.id,
                pinned         = pinned,
                attachmentBytes= attachmentBytes,
                attachmentName = attachmentName,
                attachmentType = attachmentType
            )

            result.onSuccess {
                loadPosts()
                onSuccess()
            }.onFailure {
                onError(it.message ?: "Failed to create post")
            }
        }
    }

    /** Deletes a post. Super Admin or Dept Admin only (enforced by RLS). */
    fun deletePost(postId: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePost(postId).onFailure { e ->
                onError(e.message ?: "Failed to delete post")
            }.onSuccess {
                loadPosts()
            }
        }
    }

    /** Display name for the current logged-in user. */
    fun getCurrentUserDisplayName(): String {
        val currentUser = authRepository.currentUser ?: return "Admin"
        return when (val profile = currentUser.profile) {
            is com.must.connect.data.model.DeptAdminProfile  -> profile.fullName
            is com.must.connect.data.model.SuperAdminProfile -> profile.fullName
            is com.must.connect.data.model.TeacherProfile    -> profile.fullName
            is com.must.connect.data.model.StudentProfile    -> profile.fullName
            else -> "Admin"
        }
    }
}
