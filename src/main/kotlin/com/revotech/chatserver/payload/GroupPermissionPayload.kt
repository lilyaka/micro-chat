package com.revotech.chatserver.payload

import com.revotech.chatserver.business.group.GroupSettings
import com.revotech.chatserver.business.group.UserLevelInGroup
import com.revotech.chatserver.business.group.UserGroupPermissions
import java.time.LocalDateTime

// Request DTOs
data class RestrictMessagingPayload(
    val restrictMessaging: Boolean
)

data class AllowMembersEditInfoPayload(
    val allowMembersToEditInfo: Boolean
)

data class AllowMembersPinMessagePayload(
    val allowMembersToPinMessage: Boolean
)

data class AllowMembersAddMembersPayload(
    val allowMembersToAddMembers: Boolean
)

data class GroupSettingsUpdatePayload(
    val restrictMessaging: Boolean? = null,
    val allowMembersToEditInfo: Boolean? = null,
    val allowMembersToPinMessage: Boolean? = null,
    val allowMembersToAddMembers: Boolean? = null,
    val allowMembersToChangeNickname: Boolean? = null
)

// Response DTOs
data class GroupSettingsResponse(
    val groupId: String,
    val settings: GroupSettings,
    val updatedAt: LocalDateTime,
    val updatedBy: String?
)

data class GroupMembersResponse(
    val groupId: String,
    val members: List<GroupMemberResponse>,
    val totalMembers: Int
)

data class GroupMemberResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val avatar: String,
    val level: UserLevelInGroup,
    val permissions: UserGroupPermissions,
    val isOnline: Boolean
)

data class RoleChangeResponse(
    val groupId: String,
    val targetUserId: String,
    val newRole: UserLevelInGroup,
    val changedBy: String,
    val timestamp: LocalDateTime,
    val success: Boolean,
    val message: String
)

// WebSocket Message DTOs
data class GroupSettingsUpdateMessage(
    val groupId: String,
    val settings: GroupSettings,
    val updatedBy: String,
    val action: String,
    val timestamp: LocalDateTime
)

data class RoleChangeMessage(
    val groupId: String,
    val targetUserId: String,
    val newRole: UserLevelInGroup,
    val changedBy: String,
    val action: String,
    val timestamp: LocalDateTime
)