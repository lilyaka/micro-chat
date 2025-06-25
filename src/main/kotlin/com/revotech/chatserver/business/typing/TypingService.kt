package com.revotech.chatserver.business.typing

import com.revotech.chatserver.business.CHAT_DESTINATION
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
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
    // Map<conversationId, Map<userId, TypingUser>>
    private val typingUsers = ConcurrentHashMap<String, ConcurrentHashMap<String, TypingUser>>()

    // Timeout sau 3 giây không có activity
    private val TYPING_TIMEOUT_SECONDS = 3L

    fun handleTyping(typingPayload: TypingPayload, principal: Principal) {
        val userId = principal.name
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            typingPayload.run {
                handleTypingInternal(conversationId, userId, isTyping)
            }
        }
    }

    fun handleTyping(conversationId: String, userId: String, isTyping: Boolean) {
        // Method này sẽ được gọi từ handleTyping(payload, principal) với tenant context
        handleTypingInternal(conversationId, userId, isTyping)
    }

    private fun handleTypingInternal(conversationId: String, userId: String, isTyping: Boolean) {
        println("🔥 TypingService: handleTypingInternal - conversationId=$conversationId, userId=$userId, isTyping=$isTyping")

        val user = userService.getUser(userId)
        val userName = user?.fullName ?: "Unknown User"

        println("🔥 TypingService: Found user name: $userName")

        if (isTyping) {
            startTyping(conversationId, userId, userName)
        } else {
            stopTyping(conversationId, userId)
        }

        broadcastTypingUpdate(conversationId)
    }

    private fun startTyping(conversationId: String, userId: String, userName: String) {
        println("🔥 TypingService: startTyping - conversationId=$conversationId, userId=$userId, userName=$userName")

        typingUsers.computeIfAbsent(conversationId) { ConcurrentHashMap() }
        typingUsers[conversationId]!![userId] = TypingUser(userId, userName, LocalDateTime.now())

        println("🔥 TypingService: Current typing users in conversation $conversationId: ${typingUsers[conversationId]?.size ?: 0}")
    }

    private fun stopTyping(conversationId: String, userId: String) {
        println("🔥 TypingService: stopTyping - conversationId=$conversationId, userId=$userId")

        typingUsers[conversationId]?.remove(userId)
        if (typingUsers[conversationId]?.isEmpty() == true) {
            typingUsers.remove(conversationId)
        }

        println("🔥 TypingService: Remaining typing users in conversation $conversationId: ${typingUsers[conversationId]?.size ?: 0}")
    }

    private fun broadcastTypingUpdate(conversationId: String) {
        val currentTypingUsers = typingUsers[conversationId]?.values?.toList() ?: emptyList()

        println("🔥 TypingService: Broadcasting typing update for conversation $conversationId")
        println("🔥 TypingService: Current typing users: ${currentTypingUsers.map { it.userName }}")

        val message = TypingUpdateMessage(
            conversationId = conversationId,
            typingUsers = currentTypingUsers
        )

        val destination = "$CHAT_DESTINATION/typing/$conversationId"
        println("🔥 TypingService: Broadcasting to destination: $destination")
        println("🔥 TypingService: Message content: $message")

        try {
            // Broadcast đến tất cả users trong conversation
            simpMessagingTemplate.convertAndSend(destination, message)
            println("✅ TypingService: Successfully broadcasted typing update")
        } catch (e: Exception) {
            println("❌ TypingService: Error broadcasting typing update: ${e.message}")
            e.printStackTrace()
        }
    }

    // Auto cleanup typing users sau timeout
    @Scheduled(fixedRate = 1000) // Check mỗi giây
    fun cleanupExpiredTyping() {
        val now = LocalDateTime.now()

        typingUsers.forEach { (conversationId, userMap) ->
            val expiredUsers = userMap.filterValues {
                it.startTime.plusSeconds(TYPING_TIMEOUT_SECONDS).isBefore(now)
            }

            if (expiredUsers.isNotEmpty()) {
                println("🔥 TypingService: Cleaning up ${expiredUsers.size} expired typing users in conversation $conversationId")

                expiredUsers.keys.forEach { userId ->
                    userMap.remove(userId)
                }

                if (userMap.isEmpty()) {
                    typingUsers.remove(conversationId)
                } else {
                    broadcastTypingUpdate(conversationId)
                }
            }
        }
    }

    fun getTypingUsers(conversationId: String): List<TypingUser> {
        return typingUsers[conversationId]?.values?.toList() ?: emptyList()
    }

    // Clear typing khi user send message - cần tenant context
    fun clearTyping(conversationId: String, userId: String) {
        println("🔥 TypingService: clearTyping called - conversationId=$conversationId, userId=$userId")
        handleTypingInternal(conversationId, userId, false)
    }
}