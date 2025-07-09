package com.revotech.chatserver.payload

import com.revotech.chatserver.business.group.UserLevelInGroup
import com.revotech.chatserver.business.group.UserGroupPermissions
import com.revotech.chatserver.business.group.GroupSettings

data class UserGroupPermissionResponse(
    val userId: String,
    val role: UserLevelInGroup?,
    val permissions: UserGroupPermissions
)

data class GroupDetailResponse(
    val id: String,
    val name: String,
    val isDelete: Boolean,
    val users: List<UserInGroupResponse>,
    val settings: GroupSettings,
    val createdBy: String?,
    val userPermissions: UserGroupPermissions? = null  // Current user's permissions
)

data class UserInGroupResponse(
    val id: String,
    val level: UserLevelInGroup?,
    val fullName: String?,
    val email: String?
)

data class ConversationDetailResponse(
    val id: String?,
    val name: String,
    val avatar: String?,
    val isGroup: Boolean,
    val members: List<String>,
    val groupId: String? = null,                     // ✅ NEW: Actual group ID
    val groupSettings: GroupSettings? = null,        // Group settings if isGroup
    val userPermissions: UserGroupPermissions? = null, // Current user's permissions if isGroup
    val totalAttachment: Int? = 0,
    val unread: Int? = 0
)