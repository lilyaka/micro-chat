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
    private val tenantHelper: TenantHelper
) {

    // ✅ ĐÃ CÓ MULTI-TENANT
    fun addReaction(messageId: String, emoji: String, userId: String, principal: Principal) {
        if (emoji.isBlank() || emoji.length > 10) {
            throw IllegalArgumentException("Invalid emoji format")
        }

        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val message = chatService.getMessage(messageId)

            val existing = reactionRepository.findByMessageIdAndUserId(messageId, userId)
                .find { it.emoji == emoji }

            if (existing == null) {
                val reaction = MessageReaction(null, messageId, userId, emoji)
                reactionRepository.save(reaction)
                broadcastReactionUpdate(message.conversationId, messageId)
            }
        }
    }

    // ✅ ĐÃ CÓ MULTI-TENANT
    fun removeReaction(messageId: String, emoji: String, userId: String, principal: Principal) {
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

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getReactionSummary(messageId: String, principal: Principal): List<ReactionSummary> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            reactionRepository.getReactionSummary(messageId)
        }
    }
}

data class ReactionUpdateMessage(
    val messageId: String,
    val reactions: List<ReactionSummary>
)