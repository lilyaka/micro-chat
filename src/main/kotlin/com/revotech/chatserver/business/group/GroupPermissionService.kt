package com.revotech.chatserver.business.group

import com.revotech.chatserver.business.exception.GroupPermissionException
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.payload.UserGroupPermissionResponse
import org.springframework.stereotype.Service

@Service
class GroupPermissionService(
    private val groupRepository: GroupRepository, // ✅ DIRECT dependency
    private val userService: UserService
) {

    /**
     * Tính toán quyền của user trong group
     */
    fun calculatePermissions(groupId: String, userId: String): UserGroupPermissions {
        val group = groupRepository.findById(groupId).orElse(null) // ✅ DIRECT query
            ?: return UserGroupPermissions.noPermissions()

        val userRole = group.getUserRole(userId)
            ?: return UserGroupPermissions.noPermissions()

        return UserGroupPermissions.fromRoleAndSettings(userRole, group.settings)
    }

    /**
     * Lấy role của user trong group
     */
    fun getUserRoleInGroup(groupId: String, userId: String): UserLevelInGroup? {
        val group = groupRepository.findById(groupId).orElse(null) ?: return null // ✅ DIRECT query
        return group.getUserRole(userId)
    }

    /**
     * ✅ ADDED: Get group directly
     */
    fun getGroup(groupId: String): Group? {
        return groupRepository.findById(groupId).orElse(null)
    }

    /**
     * Kiểm tra user có thể thực hiện action không
     */
    fun canSendMessage(groupId: String, userId: String): Boolean {
        val permissions = calculatePermissions(groupId, userId)
        return permissions.canSendMessage
    }

    fun canEditGroupInfo(groupId: String, userId: String): Boolean {
        val permissions = calculatePermissions(groupId, userId)
        return permissions.canEditGroupInfo
    }

    fun canPinMessage(groupId: String, userId: String): Boolean {
        val permissions = calculatePermissions(groupId, userId)
        return permissions.canPinMessage
    }

    fun canAddMembers(groupId: String, userId: String): Boolean {
        val permissions = calculatePermissions(groupId, userId)
        return permissions.canAddMembers
    }

    fun canRemoveMembers(groupId: String, userId: String): Boolean {
        val permissions = calculatePermissions(groupId, userId)
        return permissions.canRemoveMembers
    }

    fun canChangeGroupSettings(groupId: String, userId: String): Boolean {
        val permissions = calculatePermissions(groupId, userId)
        return permissions.canChangeGroupSettings
    }

    fun canPromoteToAdmin(groupId: String, userId: String): Boolean {
        val permissions = calculatePermissions(groupId, userId)
        return permissions.canPromoteToAdmin
    }

    fun canDemoteFromAdmin(groupId: String, userId: String): Boolean {
        val permissions = calculatePermissions(groupId, userId)
        return permissions.canDemoteFromAdmin
    }

    fun canDeleteGroup(groupId: String, userId: String): Boolean {
        val permissions = calculatePermissions(groupId, userId)
        return permissions.canDeleteGroup
    }

    /**
     * Kiểm tra quyền với action string (để dùng trong generic validation)
     */
    fun canPerformAction(groupId: String, userId: String, action: GroupAction): Boolean {
        return when (action) {
            GroupAction.SEND_MESSAGE -> canSendMessage(groupId, userId)
            GroupAction.EDIT_GROUP_INFO -> canEditGroupInfo(groupId, userId)
            GroupAction.PIN_MESSAGE -> canPinMessage(groupId, userId)
            GroupAction.ADD_MEMBERS -> canAddMembers(groupId, userId)
            GroupAction.REMOVE_MEMBERS -> canRemoveMembers(groupId, userId)
            GroupAction.CHANGE_SETTINGS -> canChangeGroupSettings(groupId, userId)
            GroupAction.PROMOTE_TO_ADMIN -> canPromoteToAdmin(groupId, userId)
            GroupAction.DEMOTE_FROM_ADMIN -> canDemoteFromAdmin(groupId, userId)
            GroupAction.DELETE_GROUP -> canDeleteGroup(groupId, userId)
        }
    }

    /**
     * Lấy tất cả permissions của user cho response
     */
    fun getUserPermissions(groupId: String, userId: String): UserGroupPermissionResponse {
        val permissions = calculatePermissions(groupId, userId)
        val userRole = getUserRoleInGroup(groupId, userId)

        return UserGroupPermissionResponse(
            userId = userId,
            role = userRole,
            permissions = permissions
        )
    }

    /**
     * Validate permission action với throw exception nếu không đủ quyền
     */
    fun validatePermission(groupId: String, userId: String, action: GroupAction) {
        if (!canPerformAction(groupId, userId, action)) {
            val userRole = getUserRoleInGroup(groupId, userId)
            throw GroupPermissionException(
                "permissionDenied",
                "User with role $userRole cannot perform action: $action"
            )
        }
    }

    /**
     * Bulk check permissions cho multiple actions
     */
    fun checkMultiplePermissions(groupId: String, userId: String, actions: List<GroupAction>): Map<GroupAction, Boolean> {
        return actions.associateWith { action ->
            canPerformAction(groupId, userId, action)
        }
    }
}