package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.GroupMigrationService
import com.revotech.chatserver.business.group.ConversationGroupMigrationService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/migration")
class GroupMigrationController(
    private val groupMigrationService: GroupMigrationService,
    private val conversationGroupMigrationService: ConversationGroupMigrationService
) {

    @PostMapping("/group-roles")
    fun migrateGroupRoles(): Map<String, Any> {
        val result = groupMigrationService.migrateGroupRoles()
        return mapOf(
            "success" to true,
            "message" to "Group role migration completed",
            "migratedGroups" to result.migratedCount,
            "totalGroups" to result.totalCount,
            "details" to result.details
        )
    }

    @GetMapping("/group-roles/status")
    fun checkMigrationStatus(): Map<String, Any> {
        val status = groupMigrationService.checkMigrationStatus()
        return mapOf(
            "needsMigration" to status.needsMigration,
            "groupsNeedingMigration" to status.groupsNeedingMigration,
            "totalGroups" to status.totalGroups
        )
    }

    @PostMapping("/conversation-groups")
    fun migrateConversationGroups(): Map<String, Any> {
        val result = conversationGroupMigrationService.migrateGroupConversationsToGroups()
        return mapOf(
            "success" to true,
            "message" to "Group conversation migration completed",
            "migratedGroups" to result.migratedCount,
            "totalGroupConversations" to result.totalGroupConversations,
            "details" to result.details
        )
    }

    @GetMapping("/conversation-groups/status")
    fun checkConversationGroupMigrationStatus(): Map<String, Any> {
        val status = conversationGroupMigrationService.checkGroupConversationMigrationStatus()
        return mapOf(
            "needsMigration" to status.needsMigration,
            "conversationsNeedingGroups" to status.conversationsNeedingGroups,
            "totalGroupConversations" to status.totalGroupConversations
        )
    }
}