package com.revotech.chatserver.business.group

data class UserGroupPermissions(
    val canSendMessage: Boolean,              // Gửi tin nhắn (phụ thuộc restrictMessaging)
    val canEditGroupInfo: Boolean,            // Sửa tên, avatar, mô tả nhóm
    val canPinMessage: Boolean,               // Ghim tin nhắn
    val canAddMembers: Boolean,               // Thêm thành viên
    val canRemoveMembers: Boolean,            // Xóa thành viên
    val canChangeGroupSettings: Boolean,      // Thay đổi settings nhóm (Owner + Admin)
    val canPromoteToAdmin: Boolean,           // Bổ nhiệm thành phó nhóm (Owner + Admin)
    val canDemoteFromAdmin: Boolean,          // Cắt chức phó nhóm (chỉ Owner)
    val canDeleteGroup: Boolean,              // Giải tán nhóm (chỉ Owner)
    val canChangeNickname: Boolean            // Đổi biệt danh (future feature)
) {
    companion object {
        // Helper method để tạo permissions từ role và settings
        fun fromRoleAndSettings(
            userRole: UserLevelInGroup,
            groupSettings: GroupSettings
        ): UserGroupPermissions {
            return when (userRole) {
                UserLevelInGroup.OWNER -> UserGroupPermissions(
                    canSendMessage = true,                          // Owner luôn gửi được
                    canEditGroupInfo = true,                        // Owner luôn sửa được
                    canPinMessage = true,                           // Owner luôn pin được
                    canAddMembers = true,                           // Owner luôn add được
                    canRemoveMembers = true,                        // Owner luôn remove được
                    canChangeGroupSettings = true,                  // Owner luôn change settings được
                    canPromoteToAdmin = true,                       // Owner luôn promote được
                    canDemoteFromAdmin = true,                      // ✅ Chỉ Owner mới demote được
                    canDeleteGroup = true,                          // ✅ Chỉ Owner mới delete được
                    canChangeNickname = true                        // Owner luôn đổi nickname được
                )

                UserLevelInGroup.ADMIN -> UserGroupPermissions(
                    canSendMessage = true,                          // Admin luôn gửi được
                    canEditGroupInfo = true,                        // Admin luôn sửa được
                    canPinMessage = true,                           // Admin luôn pin được
                    canAddMembers = true,                           // Admin luôn add được
                    canRemoveMembers = true,                        // Admin luôn remove được
                    canChangeGroupSettings = true,                  // ✅ Admin cũng change settings được
                    canPromoteToAdmin = true,                       // ✅ Admin cũng promote được
                    canDemoteFromAdmin = false,                     // ❌ Admin KHÔNG demote được
                    canDeleteGroup = false,                         // ❌ Admin KHÔNG delete được
                    canChangeNickname = true                        // Admin luôn đổi nickname được
                )

                UserLevelInGroup.MEMBER -> UserGroupPermissions(
                    canSendMessage = !groupSettings.restrictMessaging,           // Phụ thuộc setting
                    canEditGroupInfo = groupSettings.allowMembersToEditInfo,     // Phụ thuộc setting
                    canPinMessage = groupSettings.allowMembersToPinMessage,      // Phụ thuộc setting
                    canAddMembers = groupSettings.allowMembersToAddMembers,      // Phụ thuộc setting
                    canRemoveMembers = false,                                    // Member không remove được
                    canChangeGroupSettings = false,                              // Member không change settings được
                    canPromoteToAdmin = false,                                   // Member không promote được
                    canDemoteFromAdmin = false,                                  // Member không demote được
                    canDeleteGroup = false,                                      // Member không delete được
                    canChangeNickname = groupSettings.allowMembersToChangeNickname // Phụ thuộc setting
                )
            }
        }

        // Quick check methods cho common actions
        fun canManageGroup(userRole: UserLevelInGroup): Boolean {
            return userRole in listOf(UserLevelInGroup.OWNER, UserLevelInGroup.ADMIN)
        }

        fun canPromote(userRole: UserLevelInGroup): Boolean {
            return userRole in listOf(UserLevelInGroup.OWNER, UserLevelInGroup.ADMIN)
        }

        fun canDemote(userRole: UserLevelInGroup): Boolean {
            return userRole == UserLevelInGroup.OWNER
        }

        fun canDeleteGroup(userRole: UserLevelInGroup): Boolean {
            return userRole == UserLevelInGroup.OWNER
        }
    }
}