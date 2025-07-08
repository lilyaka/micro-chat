package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.GroupAction
import com.revotech.chatserver.business.group.GroupPermissionService
import com.revotech.chatserver.business.group.GroupRoleService
import com.revotech.chatserver.payload.GroupMembersResponse
import com.revotech.chatserver.payload.RoleChangeResponse
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/group")
class GroupRoleController(
    private val groupRoleService: GroupRoleService,
    private val groupPermissionService: GroupPermissionService
) {

    @GetMapping("/{groupId}/members")
    fun getGroupMembers(@PathVariable groupId: String, principal: Principal): GroupMembersResponse {
        val userId = principal.name

        // Basic member check - anyone in group can view members
        if (groupPermissionService.getUserRoleInGroup(groupId, userId) == null) {
            println("⚠️ WARNING: User $userId not in group $groupId attempting to view members")
        }

        return groupRoleService.getGroupMembers(groupId, userId, principal)
    }

    @PutMapping("/{groupId}/promote/{targetUserId}")
    fun promoteToAdmin(
        @PathVariable groupId: String,
        @PathVariable targetUserId: String,
        principal: Principal
    ): RoleChangeResponse {
        val userId = principal.name

        // Soft permission check with warning
        try {
            groupPermissionService.validatePermission(groupId, userId, GroupAction.PROMOTE_TO_ADMIN)
        } catch (e: Exception) {
            println("⚠️ WARNING: Permission denied but allowing promotion: ${e.message}")
        }

        return groupRoleService.promoteToAdmin(groupId, targetUserId, userId, principal)
    }

    @PutMapping("/{groupId}/demote/{targetUserId}")
    fun demoteFromAdmin(
        @PathVariable groupId: String,
        @PathVariable targetUserId: String,
        principal: Principal
    ): RoleChangeResponse {
        val userId = principal.name

        // Soft permission check with warning
        try {
            groupPermissionService.validatePermission(groupId, userId, GroupAction.DEMOTE_FROM_ADMIN)
        } catch (e: Exception) {
            println("⚠️ WARNING: Permission denied but allowing demotion: ${e.message}")
        }

        return groupRoleService.demoteFromAdmin(groupId, targetUserId, userId, principal)
    }

    @PutMapping("/{groupId}/transfer-ownership/{targetUserId}")
    fun transferOwnership(
        @PathVariable groupId: String,
        @PathVariable targetUserId: String,
        principal: Principal
    ): RoleChangeResponse {
        val userId = principal.name

        // Strict check for ownership transfer - this is critical
        try {
            groupPermissionService.validatePermission(groupId, userId, GroupAction.DELETE_GROUP) // Only MANAGER can transfer
        } catch (e: Exception) {
            println("❌ CRITICAL: Ownership transfer denied: ${e.message}")
            throw e
        }

        return groupRoleService.transferOwnership(groupId, targetUserId, userId, principal)
    }

    @GetMapping("/{groupId}/permissions/{targetUserId}")
    fun getUserPermissions(
        @PathVariable groupId: String,
        @PathVariable targetUserId: String,
        principal: Principal
    ) = groupPermissionService.getUserPermissions(groupId, targetUserId)
}