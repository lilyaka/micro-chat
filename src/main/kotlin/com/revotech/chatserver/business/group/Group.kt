package com.revotech.chatserver.business.group

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

const val DB_GROUP = "sys_group"

@Document(DB_GROUP)
data class Group(
    @Id
    val id: String,
    val name: String,
    val isDelete: Boolean = false,
    val users: MutableList<UserInGroup>,

    // ✅ NEW FIELDS for permissions
    val settings: GroupSettings = GroupSettings.defaultSettings(),  // Settings với default values
    val createdBy: String? = null,                                   // ID của người tạo nhóm
    val createdAt: LocalDateTime = LocalDateTime.now(),              // Thời gian tạo
    val updatedAt: LocalDateTime = LocalDateTime.now()               // Thời gian cập nhật cuối
) {
    // Helper method để lấy role của user trong group
    fun getUserRole(userId: String): UserLevelInGroup? {
        return users.find { it.id == userId }?.level
    }

    // Helper method để check xem user có phải owner không
    fun isOwner(userId: String): Boolean {
        val userRole = getUserRole(userId)
        val effectiveRole = userRole?.let { UserLevelInGroup.getEffectiveLevel(it) }
        return createdBy == userId || effectiveRole == UserLevelInGroup.MANAGER
    }

    // Helper method để check xem user có quyền quản lý không
    fun canManage(userId: String): Boolean {
        val role = getUserRole(userId)
        if (role == null) return false

        val effectiveRole = UserLevelInGroup.getEffectiveLevel(role)
        return effectiveRole in listOf(UserLevelInGroup.MANAGER, UserLevelInGroup.ADMIN)
    }

    // Helper method để lấy tất cả admins (owner + admins)
    fun getAllAdmins(): List<UserInGroup> {
        return users.filter {
            val effectiveLevel = UserLevelInGroup.getEffectiveLevel(it.getSafeLevel())
            effectiveLevel in listOf(UserLevelInGroup.MANAGER, UserLevelInGroup.ADMIN)
        }
    }

    // Helper method để lấy chỉ members
    fun getMembers(): List<UserInGroup> {
        return users.filter {
            val effectiveLevel = UserLevelInGroup.getEffectiveLevel(it.getSafeLevel())
            effectiveLevel == UserLevelInGroup.MEMBER
        }
    }
}

data class UserInGroup(
    val id: String,
    val level: UserLevelInGroup?,  // Có thể null cho backward compatibility
    val fullName: String?,
    val email: String?
) {
    // Helper method để get safe level với fallback
    fun getSafeLevel(): UserLevelInGroup {
        return level ?: UserLevelInGroup.MEMBER  // Default to MEMBER nếu null
    }
}