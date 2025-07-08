package com.revotech.chatserver.business.group

import com.revotech.chatserver.business.GROUP_DESTINATION
import com.revotech.chatserver.business.exception.GroupException
import com.revotech.chatserver.payload.GroupSettingsResponse
import com.revotech.chatserver.payload.GroupSettingsUpdateMessage
import com.revotech.chatserver.payload.GroupSettingsUpdatePayload
import com.revotech.util.WebUtil
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GroupSettingsService(
    private val groupRepository: GroupRepository,
    private val groupService: GroupService,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val webUtil: WebUtil
) {

    fun getGroupSettings(groupId: String): GroupSettingsResponse {
        val group = groupService.getGroup(groupId)
            ?: throw GroupException("groupNotFound", "Group not found")

        return GroupSettingsResponse(
            groupId = groupId,
            settings = group.settings,
            updatedAt = group.updatedAt,
            updatedBy = null
        )
    }

    fun updateRestrictMessaging(groupId: String, restrictMessaging: Boolean): GroupSettingsResponse {
        val userId = webUtil.getUserId()
        val group = groupService.getGroup(groupId)
            ?: throw GroupException("groupNotFound", "Group not found")

        val updatedSettings = group.settings.copy(restrictMessaging = restrictMessaging)
        val updatedGroup = group.copy(
            settings = updatedSettings,
            updatedAt = LocalDateTime.now()
        )

        groupRepository.save(updatedGroup)
        broadcastSettingsUpdate(groupId, updatedSettings, userId, "restrictMessaging")

        return GroupSettingsResponse(
            groupId = groupId,
            settings = updatedSettings,
            updatedAt = updatedGroup.updatedAt,
            updatedBy = userId
        )
    }

    fun updateAllowMembersEditInfo(groupId: String, allowMembersToEditInfo: Boolean): GroupSettingsResponse {
        val userId = webUtil.getUserId()
        val group = groupService.getGroup(groupId)
            ?: throw GroupException("groupNotFound", "Group not found")

        val updatedSettings = group.settings.copy(allowMembersToEditInfo = allowMembersToEditInfo)
        val updatedGroup = group.copy(
            settings = updatedSettings,
            updatedAt = LocalDateTime.now()
        )

        groupRepository.save(updatedGroup)
        broadcastSettingsUpdate(groupId, updatedSettings, userId, "allowMembersToEditInfo")

        return GroupSettingsResponse(
            groupId = groupId,
            settings = updatedSettings,
            updatedAt = updatedGroup.updatedAt,
            updatedBy = userId
        )
    }

    fun updateAllowMembersPinMessage(groupId: String, allowMembersToPinMessage: Boolean): GroupSettingsResponse {
        val userId = webUtil.getUserId()
        val group = groupService.getGroup(groupId)
            ?: throw GroupException("groupNotFound", "Group not found")

        val updatedSettings = group.settings.copy(allowMembersToPinMessage = allowMembersToPinMessage)
        val updatedGroup = group.copy(
            settings = updatedSettings,
            updatedAt = LocalDateTime.now()
        )

        groupRepository.save(updatedGroup)
        broadcastSettingsUpdate(groupId, updatedSettings, userId, "allowMembersToPinMessage")

        return GroupSettingsResponse(
            groupId = groupId,
            settings = updatedSettings,
            updatedAt = updatedGroup.updatedAt,
            updatedBy = userId
        )
    }

    fun updateAllowMembersAddMembers(groupId: String, allowMembersToAddMembers: Boolean): GroupSettingsResponse {
        val userId = webUtil.getUserId()
        val group = groupService.getGroup(groupId)
            ?: throw GroupException("groupNotFound", "Group not found")

        val updatedSettings = group.settings.copy(allowMembersToAddMembers = allowMembersToAddMembers)
        val updatedGroup = group.copy(
            settings = updatedSettings,
            updatedAt = LocalDateTime.now()
        )

        groupRepository.save(updatedGroup)
        broadcastSettingsUpdate(groupId, updatedSettings, userId, "allowMembersToAddMembers")

        return GroupSettingsResponse(
            groupId = groupId,
            settings = updatedSettings,
            updatedAt = updatedGroup.updatedAt,
            updatedBy = userId
        )
    }

    fun updateGroupSettings(groupId: String, payload: GroupSettingsUpdatePayload): GroupSettingsResponse {
        val userId = webUtil.getUserId()
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
        broadcastSettingsUpdate(groupId, updatedSettings, userId, "batchUpdate")

        return GroupSettingsResponse(
            groupId = groupId,
            settings = updatedSettings,
            updatedAt = updatedGroup.updatedAt,
            updatedBy = userId
        )
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
        } catch (e: Exception) {
            println("❌ Failed to broadcast settings update: ${e.message}")
        }
    }
}