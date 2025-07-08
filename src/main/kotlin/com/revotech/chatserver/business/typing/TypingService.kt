package com.revotech.chatserver.business.typing

import com.revotech.chatserver.business.CHAT_DESTINATION
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class TypingService(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val userService: UserService,
    private val tenantHelper: TenantHelper
) {
    private val typingUsers = ConcurrentHashMap<String, ConcurrentHashMap<String, TypingUser>>()

    fun handleTyping(conversationId: String, userId: String, isTyping: Boolean, principal: Principal) {
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            handleTypingInternal(conversationId, userId, isTyping, principal)
        }
    }

    private fun handleTypingInternal(conversationId: String, userId: String, isTyping: Boolean, principal: Principal) {

        val user = userService.getUser(userId)
        val userName = user?.fullName ?: "Unknown User"

        if (isTyping) {
            startTyping(conversationId, userId, userName)
        } else {
            stopTyping(conversationId, userId)
        }

        broadcastTypingUpdate(conversationId)
    }

    private fun startTyping(conversationId: String, userId: String, userName: String) {
        typingUsers.computeIfAbsent(conversationId) { ConcurrentHashMap() }
        typingUsers[conversationId]!![userId] = TypingUser(userId, userName, LocalDateTime.now())
    }

    private fun stopTyping(conversationId: String, userId: String) {

        typingUsers[conversationId]?.remove(userId)
        if (typingUsers[conversationId]?.isEmpty() == true) {
            typingUsers.remove(conversationId)
        }

    }

    private fun broadcastTypingUpdate(conversationId: String) {
        val currentTypingUsers = typingUsers[conversationId]?.values?.toList() ?: emptyList()

        val message = TypingUpdateMessage(
            conversationId = conversationId,
            typingUsers = currentTypingUsers
        )

        val destination = "$CHAT_DESTINATION/typing/$conversationId"

        try {
            simpMessagingTemplate.convertAndSend(destination, message)
        } catch (e: Exception) {
            println("❌ TypingService: Error broadcasting typing update: ${e.message}")
            e.printStackTrace()
        }
    }

    fun cleanupUserTyping(userId: String) {

        val conversationsToUpdate = mutableSetOf<String>()

        typingUsers.forEach { (conversationId, userMap) ->
            if (userMap.containsKey(userId)) {
                userMap.remove(userId)
                conversationsToUpdate.add(conversationId)

                // Remove empty conversations
                if (userMap.isEmpty()) {
                    typingUsers.remove(conversationId)
                }
            }
        }

        // Broadcast updates for affected conversations
        conversationsToUpdate.forEach { conversationId ->
            broadcastTypingUpdate(conversationId)
        }

    }

    fun cleanupConversationTyping(conversationId: String) {
        println("🔥 TypingService: cleanupConversationTyping for conversation: $conversationId")

        if (typingUsers.remove(conversationId) != null) {
            println("🔥 TypingService: Removed all typing users for conversation: $conversationId")
        }
    }

    // ✅ NEW: Cleanup typing for multiple users (bulk operation)
    fun cleanupUsersTyping(userIds: List<String>) {

        userIds.forEach { userId ->
            cleanupUserTyping(userId)
        }
    }

    // ✅ NEW: Get current typing users (for debugging/monitoring)
    fun getTypingUsers(conversationId: String): List<TypingUser> {
        return typingUsers[conversationId]?.values?.toList() ?: emptyList()
    }

    // ✅ NEW: Get all typing stats (for debugging/monitoring)
    fun getTypingStats(): Map<String, Any> {
        val totalConversations = typingUsers.size
        val totalTypingUsers = typingUsers.values.sumOf { it.size }

        return mapOf(
            "totalConversations" to totalConversations,
            "totalTypingUsers" to totalTypingUsers,
            "conversations" to typingUsers.mapValues { (_, userMap) ->
                userMap.mapValues { (_, typingUser) ->
                    mapOf(
                        "userName" to typingUser.userName,
                        "startTime" to typingUser.startTime.toString()
                    )
                }
            }
        )
    }

    fun clearTyping(conversationId: String, userId: String, principal: Principal) {
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            handleTypingInternal(conversationId, userId, false, principal)
        }
    }
}