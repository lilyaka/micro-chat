package com.revotech.chatserver.business.group

import com.revotech.chatserver.business.GROUP_DESTINATION
import com.revotech.chatserver.business.exception.GroupException
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.GroupSettingsResponse
import com.revotech.chatserver.payload.GroupSettingsUpdateMessage
import com.revotech.chatserver.payload.GroupSettingsUpdatePayload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal
import java.time.LocalDateTime

@Service
class GroupSettingsService(
    private val groupRepository: GroupRepository,
    private val groupService: GroupService,
    private val tenantHelper: TenantHelper,
    private val simpMessagingTemplate: SimpMessagingTemplate
) {

    fun getGroupSettings(groupId: String, userId: String, principal: Principal): GroupSettingsResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            GroupSettingsResponse(
                groupId = groupId,
                settings = group.settings,
                updatedAt = group.updatedAt,
                updatedBy = null // Current settings, no specific updater
            )
        }
    }

    fun updateRestrictMessaging(
        groupId: String,
        restrictMessaging: Boolean,
        userId: String,
        principal: Principal
    ): GroupSettingsResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            val updatedSettings = group.settings.copy(restrictMessaging = restrictMessaging)
            val updatedGroup = group.copy(
                settings = updatedSettings,
                updatedAt = LocalDateTime.now()
            )

            groupRepository.save(updatedGroup)

            // Broadcast setting change
            broadcastSettingsUpdate(groupId, updatedSettings, userId, "restrictMessaging")

            GroupSettingsResponse(
                groupId = groupId,
                settings = updatedSettings,
                updatedAt = updatedGroup.updatedAt,
                updatedBy = userId
            )
        }
    }

    fun updateAllowMembersEditInfo(
        groupId: String,
        allowMembersToEditInfo: Boolean,
        userId: String,
        principal: Principal
    ): GroupSettingsResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            val updatedSettings = group.settings.copy(allowMembersToEditInfo = allowMembersToEditInfo)
            val updatedGroup = group.copy(
                settings = updatedSettings,
                updatedAt = LocalDateTime.now()
            )

            groupRepository.save(updatedGroup)

            // Broadcast setting change
            broadcastSettingsUpdate(groupId, updatedSettings, userId, "allowMembersToEditInfo")

            GroupSettingsResponse(
                groupId = groupId,
                settings = updatedSettings,
                updatedAt = updatedGroup.updatedAt,
                updatedBy = userId
            )
        }
    }

    fun updateAllowMembersPinMessage(
        groupId: String,
        allowMembersToPinMessage: Boolean,
        userId: String,
        principal: Principal
    ): GroupSettingsResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            val updatedSettings = group.settings.copy(allowMembersToPinMessage = allowMembersToPinMessage)
            val updatedGroup = group.copy(
                settings = updatedSettings,
                updatedAt = LocalDateTime.now()
            )

            groupRepository.save(updatedGroup)

            // Broadcast setting change
            broadcastSettingsUpdate(groupId, updatedSettings, userId, "allowMembersToPinMessage")

            GroupSettingsResponse(
                groupId = groupId,
                settings = updatedSettings,
                updatedAt = updatedGroup.updatedAt,
                updatedBy = userId
            )
        }
    }

    fun updateAllowMembersAddMembers(
        groupId: String,
        allowMembersToAddMembers: Boolean,
        userId: String,
        principal: Principal
    ): GroupSettingsResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            val updatedSettings = group.settings.copy(allowMembersToAddMembers = allowMembersToAddMembers)
            val updatedGroup = group.copy(
                settings = updatedSettings,
                updatedAt = LocalDateTime.now()
            )

            groupRepository.save(updatedGroup)

            // Broadcast setting change
            broadcastSettingsUpdate(groupId, updatedSettings, userId, "allowMembersToAddMembers")

            GroupSettingsResponse(
                groupId = groupId,
                settings = updatedSettings,
                updatedAt = updatedGroup.updatedAt,
                updatedBy = userId
            )
        }
    }

    fun updateGroupSettings(
        groupId: String,
        payload: GroupSettingsUpdatePayload,
        userId: String,
        principal: Principal
    ): GroupSettingsResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            val updatedSettings = GroupSettings(
                restrictMessaging = payload.restrictMessaging ?: group.settings.restrictMessaging,
                allowMembersToEditInfo = payload.allowMembersToEditInfo ?: group.settings.allowMembersToEditInfo,
                allowMembersToPinMessage = payload.allowMembersToPinMessage ?: group.settings.allowMembersToPinMessage,
                allowMembersToAddMembers = payload.allowMembersToAddMembers ?: group.settings.allowMembersToAddMembers,
                allowMembersToChangeNickname = payload.allowMembersToChangeNickname ?: group.settings.allowMembersToChangeNickname
            )

            val updatedGroup = group.copy(
                settings = updatedSettings,
                updatedAt = LocalDateTime.now()
            )

            groupRepository.save(updatedGroup)

            // Broadcast batch setting changes
            broadcastSettingsUpdate(groupId, updatedSettings, userId, "batchUpdate")

            GroupSettingsResponse(
                groupId = groupId,
                settings = updatedSettings,
                updatedAt = updatedGroup.updatedAt,
                updatedBy = userId
            )
        }
    }

    private fun broadcastSettingsUpdate(
        groupId: String,
        settings: GroupSettings,
        updatedBy: String,
        action: String
    ) {
        val message = GroupSettingsUpdateMessage(
            groupId = groupId,
            settings = settings,
            updatedBy = updatedBy,
            action = action,
            timestamp = LocalDateTime.now()
        )

        try {
            simpMessagingTemplate.convertAndSend(
                "$GROUP_DESTINATION/$groupId/settings",
                message
            )
            println("✅ Broadcasted settings update for group $groupId: $action")
        } catch (e: Exception) {
            println("❌ Failed to broadcast settings update: ${e.message}")
        }
    }
}