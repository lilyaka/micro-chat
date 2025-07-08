package com.revotech.chatserver.business.group

import java.time.LocalDateTime

object GroupMigrationHelper {

    /**
     * Migrate existing groups to new permission system
     * - Map MANAGE → OWNER
     * - Set legacy settings for existing groups
     * - Set createdBy if possible
     */
    fun migrateExistingGroup(group: Group): Group {
        val migratedUsers = group.users.map { userInGroup ->
            val newLevel = when (userInGroup.level) {
                UserLevelInGroup.MEMBER -> UserLevelInGroup.MEMBER
                null -> UserLevelInGroup.MEMBER  // Default cho null
                else -> UserLevelInGroup.OWNER   // Map old MANAGE → OWNER
            }
            userInGroup.copy(level = newLevel)
        }.toMutableList()

        // Tìm owner từ danh sách users (lấy user đầu tiên có level OWNER)
        val ownerId = migratedUsers.find { it.level == UserLevelInGroup.OWNER }?.id

        return group.copy(
            users = migratedUsers,
            settings = GroupSettings.legacySettings(),  // Liberal settings cho group cũ
            createdBy = ownerId,                         // Set owner
            createdAt = LocalDateTime.now(),             // Set creation time
            updatedAt = LocalDateTime.now()              // Set update time
        )
    }

    /**
     * Validate group after migration
     */
    fun validateMigratedGroup(group: Group): List<String> {
        val errors = mutableListOf<String>()

        // Must have at least one owner
        val ownerCount = group.users.count { it.level == UserLevelInGroup.OWNER }
        if (ownerCount == 0) {
            errors.add("Group must have at least one owner")
        }

        // CreatedBy should be an owner
        if (group.createdBy != null && !group.isOwner(group.createdBy)) {
            errors.add("CreatedBy user should be an owner")
        }

        // All users should have valid levels
        val usersWithoutLevel = group.users.filter { it.level == null }
        if (usersWithoutLevel.isNotEmpty()) {
            errors.add("Some users have null level: ${usersWithoutLevel.map { it.id }}")
        }

        return errors
    }
}