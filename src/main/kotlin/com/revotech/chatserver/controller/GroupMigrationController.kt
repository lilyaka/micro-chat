package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.GroupMigrationService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/migration")
class GroupMigrationController(
    private val groupMigrationService: GroupMigrationService
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
}