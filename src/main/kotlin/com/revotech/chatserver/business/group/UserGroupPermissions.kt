package com.revotech.chatserver.business.group

data class UserGroupPermissions(
    val canSendMessage: Boolean,
    val canEditGroupInfo: Boolean,
    val canPinMessage: Boolean,
    val canAddMembers: Boolean,
    val canRemoveMembers: Boolean,
    val canChangeGroupSettings: Boolean,
    val canPromoteToAdmin: Boolean,
    val canDemoteFromAdmin: Boolean,
    val canDeleteGroup: Boolean,
    val canChangeNickname: Boolean
) {
    companion object {
        // Helper method để tạo permissions từ role và settings
        fun fromRoleAndSettings(
            userRole: UserLevelInGroup,
            groupSettings: GroupSettings
        ): UserGroupPermissions {
            // ✅ Handle MANAGE as MANAGER for permissions
            val effectiveRole = UserLevelInGroup.getEffectiveLevel(userRole)

            return when (effectiveRole) {
                UserLevelInGroup.MANAGER -> UserGroupPermissions(
                    canSendMessage = true,
                    canEditGroupInfo = true,
                    canPinMessage = true,
                    canAddMembers = true,
                    canRemoveMembers = true,
                    canChangeGroupSettings = true,
                    canPromoteToAdmin = true,
                    canDemoteFromAdmin = true,
                    canDeleteGroup = true,
                    canChangeNickname = true
                )

                UserLevelInGroup.ADMIN -> UserGroupPermissions(
                    canSendMessage = true,
                    canEditGroupInfo = true,
                    canPinMessage = true,
                    canAddMembers = true,
                    canRemoveMembers = true,
                    canChangeGroupSettings = true,
                    canPromoteToAdmin = true,
                    canDemoteFromAdmin = false,
                    canDeleteGroup = false,
                    canChangeNickname = true
                )

                UserLevelInGroup.MEMBER -> UserGroupPermissions(
                    canSendMessage = !groupSettings.restrictMessaging,
                    canEditGroupInfo = groupSettings.allowMembersToEditInfo,
                    canPinMessage = groupSettings.allowMembersToPinMessage,
                    canAddMembers = groupSettings.allowMembersToAddMembers,
                    canRemoveMembers = false,
                    canChangeGroupSettings = false,
                    canPromoteToAdmin = false,
                    canDemoteFromAdmin = false,
                    canDeleteGroup = false,
                    canChangeNickname = groupSettings.allowMembersToChangeNickname
                )

                else -> noPermissions() // Fallback for any unknown roles
            }
        }

        // No permissions cho user không thuộc group
        fun noPermissions(): UserGroupPermissions {
            return UserGroupPermissions(
                canSendMessage = false,
                canEditGroupInfo = false,
                canPinMessage = false,
                canAddMembers = false,
                canRemoveMembers = false,
                canChangeGroupSettings = false,
                canPromoteToAdmin = false,
                canDemoteFromAdmin = false,
                canDeleteGroup = false,
                canChangeNickname = false
            )
        }

        // Quick check methods cho common actions
        fun canManageGroup(userRole: UserLevelInGroup): Boolean {
            val effectiveRole = UserLevelInGroup.getEffectiveLevel(userRole)
            return effectiveRole in listOf(UserLevelInGroup.MANAGER, UserLevelInGroup.ADMIN)
        }

        fun canPromote(userRole: UserLevelInGroup): Boolean {
            val effectiveRole = UserLevelInGroup.getEffectiveLevel(userRole)
            return effectiveRole in listOf(UserLevelInGroup.MANAGER, UserLevelInGroup.ADMIN)
        }

        fun canDemote(userRole: UserLevelInGroup): Boolean {
            val effectiveRole = UserLevelInGroup.getEffectiveLevel(userRole)
            return effectiveRole == UserLevelInGroup.MANAGER
        }

        fun canDeleteGroup(userRole: UserLevelInGroup): Boolean {
            val effectiveRole = UserLevelInGroup.getEffectiveLevel(userRole)
            return effectiveRole == UserLevelInGroup.MANAGER
        }
    }
}