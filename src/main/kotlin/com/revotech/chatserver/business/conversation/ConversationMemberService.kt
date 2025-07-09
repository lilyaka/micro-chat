package com.revotech.chatserver.business.conversation

import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.group.GroupPermissionService
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.payload.ConversationMemberResponse
import org.springframework.stereotype.Service

@Service
class ConversationMemberService(
    private val chatService: ChatService,
    private val userService: UserService,
    private val groupPermissionService: GroupPermissionService
) {

    fun getConversationMembersWithPermissions(conversationId: String): List<ConversationMemberResponse> {
        val conversation = chatService.getConversation(conversationId)
        val members = userService.getConversationMembers(conversationId)

        return members.map { user ->
            val role = if (conversation.isGroup) {
                groupPermissionService.getUserRoleInGroup(conversationId, user.id)
            } else null

            val permissions = if (conversation.isGroup) {
                groupPermissionService.calculatePermissions(conversationId, user.id)
            } else null

            ConversationMemberResponse(
                id = user.id,
                fullName = user.fullName,
                email = user.email,
                avatar = user.avatar,
                role = role,
                permissions = permissions,
                isOnline = false // TODO: integrate with presence service
            )
        }
    }
}