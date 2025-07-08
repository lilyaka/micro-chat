package com.revotech.chatserver.business.group

data class GroupSettings(
    val restrictMessaging: Boolean = false,         // Chỉ owner/admin được gửi tin nhắn
    val allowMembersToEditInfo: Boolean = false,    // Cho phép member sửa thông tin nhóm
    val allowMembersToPinMessage: Boolean = false,  // Cho phép member ghim tin nhắn
    val allowMembersToAddMembers: Boolean = false,  // Cho phép member thêm người (future)
    val allowMembersToChangeNickname: Boolean = true // Cho phép đổi biệt danh (future)
) {
    companion object {
        // Default settings cho nhóm mới
        fun defaultSettings(): GroupSettings {
            return GroupSettings(
                restrictMessaging = false,              // Mặc định ai cũng gửi được
                allowMembersToEditInfo = false,         // Mặc định chỉ admin sửa được
                allowMembersToPinMessage = false,       // Mặc định chỉ admin pin được
                allowMembersToAddMembers = false,       // Mặc định chỉ admin add được
                allowMembersToChangeNickname = true     // Mặc định ai cũng đổi nickname được
            )
        }

        // Migration settings cho group cũ - liberal hơn để tránh break existing behavior
        fun legacySettings(): GroupSettings {
            return GroupSettings(
                restrictMessaging = false,              // Giữ nguyên behavior cũ
                allowMembersToEditInfo = true,          // Liberal cho group cũ
                allowMembersToPinMessage = true,        // Liberal cho group cũ
                allowMembersToAddMembers = false,       // Conservative
                allowMembersToChangeNickname = true
            )
        }
    }
}