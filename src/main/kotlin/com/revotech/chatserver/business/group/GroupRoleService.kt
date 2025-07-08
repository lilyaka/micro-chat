package com.revotech.chatserver.business.group

import com.revotech.chatserver.business.GROUP_DESTINATION
import com.revotech.chatserver.business.exception.GroupException
import com.revotech.chatserver.business.exception.GroupPermissionException
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.GroupMembersResponse
import com.revotech.chatserver.payload.GroupMemberResponse
import com.revotech.chatserver.payload.RoleChangeMessage
import com.revotech.chatserver.payload.RoleChangeResponse
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal
import java.time.LocalDateTime

@Service
class GroupRoleService(
    private val groupRepository: GroupRepository,
    private val groupService: GroupService,
    private val userService: UserService,
    private val tenantHelper: TenantHelper,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val groupPermissionService: GroupPermissionService
) {

    fun getGroupMembers(groupId: String, userId: String, principal: Principal): GroupMembersResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            val members = group.users.map { userInGroup ->
                val user = userService.getUser(userInGroup.id)
                val permissions = groupPermissionService.calculatePermissions(groupId, userInGroup.id)

                GroupMemberResponse(
                    id = userInGroup.id,
                    fullName = user?.fullName ?: userInGroup.fullName ?: "",
                    email = user?.email ?: userInGroup.email ?: "",
                    avatar = user?.avatar ?: "",
                    level = userInGroup.level ?: UserLevelInGroup.MEMBER,
                    permissions = permissions,
                    isOnline = false // TODO: integrate with presence service
                )
            }

            GroupMembersResponse(
                groupId = groupId,
                members = members,
                totalMembers = members.size
            )
        }
    }

    fun promoteToAdmin(
        groupId: String,
        targetUserId: String,
        userId: String,
        principal: Principal
    ): RoleChangeResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            // Find and update target user
            val updatedUsers = group.users.map { userInGroup ->
                if (userInGroup.id == targetUserId) {
                    userInGroup.copy(level = UserLevelInGroup.ADMIN)
                } else {
                    userInGroup
                }
            }.toMutableList()

            val updatedGroup = group.copy(
                users = updatedUsers,
                updatedAt = LocalDateTime.now()
            )

            groupRepository.save(updatedGroup)

            // Broadcast role change
            broadcastRoleChange(groupId, targetUserId, UserLevelInGroup.ADMIN, userId, "promote")

            RoleChangeResponse(
                groupId = groupId,
                targetUserId = targetUserId,
                newRole = UserLevelInGroup.ADMIN,
                changedBy = userId,
                timestamp = LocalDateTime.now(),
                success = true,
                message = "User promoted to admin successfully"
            )
        }
    }

    fun demoteFromAdmin(
        groupId: String,
        targetUserId: String,
        userId: String,
        principal: Principal
    ): RoleChangeResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            // Prevent demoting the group owner/creator
            if (group.createdBy == targetUserId) {
                throw GroupPermissionException("cannotDemoteOwner", "Cannot demote group owner")
            }

            // Find and update target user
            val updatedUsers = group.users.map { userInGroup ->
                if (userInGroup.id == targetUserId) {
                    userInGroup.copy(level = UserLevelInGroup.MEMBER)
                } else {
                    userInGroup
                }
            }.toMutableList()

            val updatedGroup = group.copy(
                users = updatedUsers,
                updatedAt = LocalDateTime.now()
            )

            groupRepository.save(updatedGroup)

            // Broadcast role change
            broadcastRoleChange(groupId, targetUserId, UserLevelInGroup.MEMBER, userId, "demote")

            RoleChangeResponse(
                groupId = groupId,
                targetUserId = targetUserId,
                newRole = UserLevelInGroup.MEMBER,
                changedBy = userId,
                timestamp = LocalDateTime.now(),
                success = true,
                message = "User demoted to member successfully"
            )
        }
    }

    fun transferOwnership(
        groupId: String,
        targetUserId: String,
        userId: String,
        principal: Principal
    ): RoleChangeResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val group = groupService.getGroup(groupId)
                ?: throw GroupException("groupNotFound", "Group not found")

            // Update roles: current owner becomes admin, target becomes owner
            val updatedUsers = group.users.map { userInGroup ->
                when (userInGroup.id) {
                    userId -> userInGroup.copy(level = UserLevelInGroup.ADMIN) // Current owner becomes admin
                    targetUserId -> userInGroup.copy(level = UserLevelInGroup.MANAGER) // Target becomes owner
                    else -> userInGroup
                }
            }.toMutableList()

            val updatedGroup = group.copy(
                users = updatedUsers,
                createdBy = targetUserId, // Transfer ownership
                updatedAt = LocalDateTime.now()
            )

            groupRepository.save(updatedGroup)

            // Broadcast ownership transfer
            broadcastRoleChange(groupId, targetUserId, UserLevelInGroup.MANAGER, userId, "transfer_ownership")
            broadcastRoleChange(groupId, userId, UserLevelInGroup.ADMIN, targetUserId, "ownership_transferred")

            RoleChangeResponse(
                groupId = groupId,
                targetUserId = targetUserId,
                newRole = UserLevelInGroup.MANAGER,
                changedBy = userId,
                timestamp = LocalDateTime.now(),
                success = true,
                message = "Ownership transferred successfully"
            )
        }
    }

    private fun broadcastRoleChange(
        groupId: String,
        targetUserId: String,
        newRole: UserLevelInGroup,
        changedBy: String,
        action: String
    ) {
        val message = RoleChangeMessage(
            groupId = groupId,
            targetUserId = targetUserId,
            newRole = newRole,
            changedBy = changedBy,
            action = action,
            timestamp = LocalDateTime.now()
        )

        try {
            simpMessagingTemplate.convertAndSend(
                "$GROUP_DESTINATION/$groupId/role",
                message
            )
            println("✅ Broadcasted role change for group $groupId: $action")
        } catch (e: Exception) {
            println("❌ Failed to broadcast role change: ${e.message}")
        }
    }
}