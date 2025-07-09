package com.revotech.chatserver.payload

import com.revotech.chatserver.business.group.UserLevelInGroup
import com.revotech.chatserver.business.group.UserGroupPermissions

data class ConversationMemberResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val avatar: String?,
    val role: UserLevelInGroup? = null, // null cho 1-on-1 conversation
    val permissions: UserGroupPermissions? = null, // null cho 1-on-1 conversation
    val isOnline: Boolean
)