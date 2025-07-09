package com.revotech.chatserver.business.group

import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GroupMigrationService(
    private val groupRepository: GroupRepository
) {

    fun migrateGroupRoles(): MigrationResult {
        println("🔄 Starting group role migration...")

        val groups = groupRepository.findAll()
        var migratedCount = 0
        val details = mutableListOf<String>()

        groups.forEach { group ->
            var needsUpdate = false

            val updatedUsers = group.users.mapIndexed { index, userInGroup ->
                if (userInGroup.level == null) {
                    needsUpdate = true
                    val newLevel = when {
                        group.createdBy == userInGroup.id -> UserLevelInGroup.MANAGER
                        index == 0 -> UserLevelInGroup.MANAGER // First user = manager
                        else -> UserLevelInGroup.MEMBER
                    }

                    val message = "Migrated user ${userInGroup.fullName} (${userInGroup.id}) to $newLevel in group ${group.name} (${group.id})"
                    println("  - $message")
                    details.add(message)

                    userInGroup.copy(level = newLevel)
                } else {
                    userInGroup
                }
            }.toMutableList()

            if (needsUpdate) {
                val updatedGroup = group.copy(
                    users = updatedUsers,
                    updatedAt = LocalDateTime.now()
                )
                groupRepository.save(updatedGroup)
                migratedCount++

                val groupMessage = "Updated group: ${group.name} (${group.id})"
                println("  ✅ $groupMessage")
                details.add(groupMessage)
            }
        }

        println("✅ Group role migration completed: $migratedCount groups updated out of ${groups.size} total")

        return MigrationResult(
            migratedCount = migratedCount,
            totalCount = groups.size,
            details = details
        )
    }

    fun checkMigrationStatus(): MigrationStatus {
        val groups = groupRepository.findAll()
        val groupsNeedingMigration = groups.count { group ->
            group.users.any { it.level == null }
        }

        return MigrationStatus(
            needsMigration = groupsNeedingMigration > 0,
            groupsNeedingMigration = groupsNeedingMigration,
            totalGroups = groups.size
        )
    }
}

data class MigrationResult(
    val migratedCount: Int,
    val totalCount: Int,
    val details: List<String>
)

data class MigrationStatus(
    val needsMigration: Boolean,
    val groupsNeedingMigration: Int,
    val totalGroups: Int
)