package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.GroupPermissionService
import com.revotech.chatserver.business.group.GroupRoleService
import com.revotech.chatserver.payload.GroupMembersResponse
import com.revotech.chatserver.payload.RoleChangeResponse
import com.revotech.chatserver.payload.UserGroupPermissionResponse
import com.revotech.util.WebUtil
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/conversation")
class GroupRoleController(
    private val groupRoleService: GroupRoleService,
    private val groupPermissionService: GroupPermissionService,
    private val webUtil: WebUtil
) {

    @GetMapping("/{groupId}/members")
    fun getGroupMembers(@PathVariable groupId: String): GroupMembersResponse {
        return groupRoleService.getGroupMembers(groupId)
    }

    @PutMapping("/{groupId}/promote/{targetUserId}")
    fun promoteToAdmin(
        @PathVariable groupId: String,
        @PathVariable targetUserId: String
    ): RoleChangeResponse {
        return groupRoleService.promoteToAdmin(groupId, targetUserId)
    }

    @PutMapping("/{groupId}/demote/{targetUserId}")
    fun demoteFromAdmin(
        @PathVariable groupId: String,
        @PathVariable targetUserId: String
    ): RoleChangeResponse {
        return groupRoleService.demoteFromAdmin(groupId, targetUserId)
    }

    @PutMapping("/{groupId}/transfer-ownership/{targetUserId}")
    fun transferOwnership(
        @PathVariable groupId: String,
        @PathVariable targetUserId: String
    ): RoleChangeResponse {
        return groupRoleService.transferOwnership(groupId, targetUserId)
    }

    @GetMapping("/{groupId}/permissions/{targetUserId}")
    fun getUserPermissions(
        @PathVariable groupId: String,
        @PathVariable targetUserId: String
    ): UserGroupPermissionResponse {
        return groupPermissionService.getUserPermissions(groupId, targetUserId)
    }
}