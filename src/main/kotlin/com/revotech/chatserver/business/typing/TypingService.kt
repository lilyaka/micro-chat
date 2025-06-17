package com.revotech.chatserver.business.typing

import com.revotech.chatserver.business.CHAT_DESTINATION
import com.revotech.chatserver.business.user.UserService
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class TypingService(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val userService: UserService
) {
    // Map<conversationId, Map<userId, TypingUser>>
    private val typingUsers = ConcurrentHashMap<String, ConcurrentHashMap<String, TypingUser>>()

    // Timeout sau 3 giây không có activity
    private val TYPING_TIMEOUT_SECONDS = 3L

    fun handleTyping(conversationId: String, userId: String, isTyping: Boolean) {
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

        // Broadcast đến tất cả users trong conversation
        simpMessagingTemplate.convertAndSend(
            "$CHAT_DESTINATION/typing/$conversationId",
            message
        )
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

    // Clear typing khi user send message
    fun clearTyping(conversationId: String, userId: String) {
        stopTyping(conversationId, userId)
        broadcastTypingUpdate(conversationId)
    }
}