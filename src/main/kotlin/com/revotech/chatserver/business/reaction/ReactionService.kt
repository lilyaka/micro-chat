package com.revotech.chatserver.business.reaction

import com.revotech.chatserver.business.ChatService
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class MessageReactionService(
    private val reactionRepository: MessageReactionRepository,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService
) {

    fun addReaction(messageId: String, emoji: String, userId: String) {
        // Kiểm tra message tồn tại
        val message = chatService.getMessage(messageId)

        // Kiểm tra user đã react với emoji này chưa
        val existing = reactionRepository.findByMessageIdAndUserId(messageId, userId)
            .find { it.emoji == emoji }

        if (existing == null) {
            val reaction = MessageReaction(null, messageId, userId, emoji)
            reactionRepository.save(reaction)

            // Broadcast reaction update
            val summary = reactionRepository.getReactionSummary(messageId)
            simpMessagingTemplate.convertAndSend(
                "/chat/reaction/${message.conversationId}",
                ReactionUpdateMessage(messageId, summary)
            )
        }
    }

    fun removeReaction(messageId: String, emoji: String, userId: String) {
        reactionRepository.deleteByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)

        val message = chatService.getMessage(messageId)
        val summary = reactionRepository.getReactionSummary(messageId)

        simpMessagingTemplate.convertAndSend(
            "/chat/reaction/${message.conversationId}",
            ReactionUpdateMessage(messageId, summary)
        )
    }
}

data class ReactionUpdateMessage(
    val messageId: String,
    val reactions: List<ReactionSummary>
)