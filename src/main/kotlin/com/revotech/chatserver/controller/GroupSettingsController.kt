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

@RestController
@RequestMapping("/group")
class GroupSettingsController(
    private val groupSettingsService: GroupSettingsService,
    private val groupPermissionService: GroupPermissionService
) {

    @GetMapping("/{groupId}/settings")
    fun getGroupSettings(@PathVariable groupId: String): GroupSettingsResponse {
        return groupSettingsService.getGroupSettings(groupId)
    }

    @PutMapping("/{groupId}/settings/restrict-messaging")
    fun updateRestrictMessaging(
        @PathVariable groupId: String,
        @RequestBody payload: RestrictMessagingPayload
    ): GroupSettingsResponse {
        return groupSettingsService.updateRestrictMessaging(groupId, payload.restrictMessaging)
    }

    @PutMapping("/{groupId}/settings/allow-members-edit-info")
    fun updateAllowMembersEditInfo(
        @PathVariable groupId: String,
        @RequestBody payload: AllowMembersEditInfoPayload
    ): GroupSettingsResponse {
        return groupSettingsService.updateAllowMembersEditInfo(groupId, payload.allowMembersToEditInfo)
    }

    @PutMapping("/{groupId}/settings/allow-members-pin-message")
    fun updateAllowMembersPinMessage(
        @PathVariable groupId: String,
        @RequestBody payload: AllowMembersPinMessagePayload
    ): GroupSettingsResponse {
        return groupSettingsService.updateAllowMembersPinMessage(groupId, payload.allowMembersToPinMessage)
    }

    @PutMapping("/{groupId}/settings/allow-members-add-members")
    fun updateAllowMembersAddMembers(
        @PathVariable groupId: String,
        @RequestBody payload: AllowMembersAddMembersPayload
    ): GroupSettingsResponse {
        return groupSettingsService.updateAllowMembersAddMembers(groupId, payload.allowMembersToAddMembers)
    }

    @PutMapping("/{groupId}/settings/batch")
    fun updateGroupSettingsBatch(
        @PathVariable groupId: String,
        @RequestBody payload: GroupSettingsUpdatePayload
    ): GroupSettingsResponse {
        return groupSettingsService.updateGroupSettings(groupId, payload)
    }
}