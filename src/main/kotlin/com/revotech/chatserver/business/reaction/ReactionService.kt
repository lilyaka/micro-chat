package com.revotech.chatserver.business.reaction

import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.helper.TenantHelper
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal

@Service
class MessageReactionService(
    private val reactionRepository: MessageReactionRepository,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
    private val tenantHelper: TenantHelper // ✅ Thêm TenantHelper
) {

    fun addReaction(messageId: String, emoji: String, userId: String, principal: Principal) {
        // Validation emoji format
        if (emoji.isBlank() || emoji.length > 10) {
            throw IllegalArgumentException("Invalid emoji format")
        }

        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            // Kiểm tra message tồn tại
            val message = chatService.getMessage(messageId)

            // Kiểm tra user đã react với emoji này chưa
            val existing = reactionRepository.findByMessageIdAndUserId(messageId, userId)
                .find { it.emoji == emoji }

            if (existing == null) {
                val reaction = MessageReaction(null, messageId, userId, emoji)
                reactionRepository.save(reaction)

                // Broadcast reaction update
                broadcastReactionUpdate(message.conversationId, messageId)
            }
        }
    }

    fun removeReaction(messageId: String, emoji: String, userId: String, principal: Principal) {
        // ✅ Wrap trong tenant context
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            reactionRepository.deleteByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)

            val message = chatService.getMessage(messageId)
            val summary = reactionRepository.getReactionSummary(messageId)

            simpMessagingTemplate.convertAndSend(
                "/chat/reaction/${message.conversationId}",
                ReactionUpdateMessage(messageId, summary)
            )
        }
    }

    private fun broadcastReactionUpdate(conversationId: String, messageId: String) {
        val summary = reactionRepository.getReactionSummary(messageId)
        simpMessagingTemplate.convertAndSend(
            "/chat/reaction/$conversationId",
            ReactionUpdateMessage(messageId, summary)
        )
    }

    fun getReactionSummary(messageId: String): List<ReactionSummary> {
        return reactionRepository.getReactionSummary(messageId)
    }
}

data class ReactionUpdateMessage(
    val messageId: String,
    val reactions: List<ReactionSummary>
)