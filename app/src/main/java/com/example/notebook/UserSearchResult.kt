package com.example.notebook

data class UserSearchResult(
    val uid: String,
    val username: String,
    val displayName: String,
    val avatarBase64: String?,
    val isPrivate: Boolean,
    var followState: FollowState
)

enum class FollowState {
    NONE,       // show "Follow" or "Request"
    REQUESTED,  // show "Requested" (tap to cancel)
    FOLLOWING   // show "Following" (tap to unfollow)
}