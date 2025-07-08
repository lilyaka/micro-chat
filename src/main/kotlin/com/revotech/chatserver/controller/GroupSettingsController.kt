package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.GroupAction
import com.revotech.chatserver.business.group.GroupPermissionService
import com.revotech.chatserver.business.group.GroupSettingsService
import com.revotech.chatserver.payload.AllowMembersAddMembersPayload
import com.revotech.chatserver.payload.AllowMembersEditInfoPayload
import com.revotech.chatserver.payload.AllowMembersPinMessagePayload
import com.revotech.chatserver.payload.GroupSettingsUpdatePayload
import com.revotech.chatserver.payload.GroupSettingsResponse
import com.revotech.chatserver.payload.RestrictMessagingPayload
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/conversation")
class GroupSettingsController(
    private val groupSettingsService: GroupSettingsService,
    private val groupPermissionService: GroupPermissionService
) {

    @GetMapping("/{groupId}/settings")
    fun getGroupSettings(@PathVariable groupId: String, principal: Principal): GroupSettingsResponse {
        val userId = principal.name

        // Soft permission check - log but allow
        val canView = groupPermissionService.canPerformAction(groupId, userId, GroupAction.CHANGE_SETTINGS)
        if (!canView) {
            println("⚠️ WARNING: User $userId attempted to view settings for group $groupId without permission")
        }

        return groupSettingsService.getGroupSettings(groupId, userId, principal)
    }

    @PutMapping("/{groupId}/settings/restrict-messaging")
    fun updateRestrictMessaging(
        @PathVariable groupId: String,
        @RequestBody payload: RestrictMessagingPayload,
        principal: Principal
    ): GroupSettingsResponse {
        val userId = principal.name

        // Soft permission check with warning
        try {
            groupPermissionService.validatePermission(groupId, userId, GroupAction.CHANGE_SETTINGS)
        } catch (e: Exception) {
            println("⚠️ WARNING: Permission denied but allowing: ${e.message}")
        }

        return groupSettingsService.updateRestrictMessaging(groupId, payload.restrictMessaging, userId, principal)
    }

    @PutMapping("/{groupId}/settings/allow-members-edit-info")
    fun updateAllowMembersEditInfo(
        @PathVariable groupId: String,
        @RequestBody payload: AllowMembersEditInfoPayload,
        principal: Principal
    ): GroupSettingsResponse {
        val userId = principal.name

        // Soft permission check with warning
        try {
            groupPermissionService.validatePermission(groupId, userId, GroupAction.CHANGE_SETTINGS)
        } catch (e: Exception) {
            println("⚠️ WARNING: Permission denied but allowing: ${e.message}")
        }

        return groupSettingsService.updateAllowMembersEditInfo(groupId, payload.allowMembersToEditInfo, userId, principal)
    }

    @PutMapping("/{groupId}/settings/allow-members-pin-message")
    fun updateAllowMembersPinMessage(
        @PathVariable groupId: String,
        @RequestBody payload: AllowMembersPinMessagePayload,
        principal: Principal
    ): GroupSettingsResponse {
        val userId = principal.name

        // Soft permission check with warning
        try {
            groupPermissionService.validatePermission(groupId, userId, GroupAction.CHANGE_SETTINGS)
        } catch (e: Exception) {
            println("⚠️ WARNING: Permission denied but allowing: ${e.message}")
        }

        return groupSettingsService.updateAllowMembersPinMessage(groupId, payload.allowMembersToPinMessage, userId, principal)
    }

    @PutMapping("/{groupId}/settings/allow-members-add-members")
    fun updateAllowMembersAddMembers(
        @PathVariable groupId: String,
        @RequestBody payload: AllowMembersAddMembersPayload,
        principal: Principal
    ): GroupSettingsResponse {
        val userId = principal.name

        // Soft permission check with warning
        try {
            groupPermissionService.validatePermission(groupId, userId, GroupAction.CHANGE_SETTINGS)
        } catch (e: Exception) {
            println("⚠️ WARNING: Permission denied but allowing: ${e.message}")
        }

        return groupSettingsService.updateAllowMembersAddMembers(groupId, payload.allowMembersToAddMembers, userId, principal)
    }

    @PutMapping("/{groupId}/settings/batch")
    fun updateGroupSettingsBatch(
        @PathVariable groupId: String,
        @RequestBody payload: GroupSettingsUpdatePayload,
        principal: Principal
    ): GroupSettingsResponse {
        val userId = principal.name

        // Soft permission check with warning
        try {
            groupPermissionService.validatePermission(groupId, userId, GroupAction.CHANGE_SETTINGS)
        } catch (e: Exception) {
            println("⚠️ WARNING: Permission denied but allowing: ${e.message}")
        }

        return groupSettingsService.updateGroupSettings(groupId, payload, userId, principal)
    }
}