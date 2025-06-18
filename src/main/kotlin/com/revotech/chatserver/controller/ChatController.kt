package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.groupCommentFile.GroupCommentFileService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.message.status.MessageStatusService
import com.revotech.chatserver.business.presence.UserPresenceService
import com.revotech.chatserver.business.reaction.MessageReactionService
import com.revotech.chatserver.business.thread.MessageThreadService
import com.revotech.chatserver.business.topicComment.TopicCommentService
import com.revotech.chatserver.business.typing.TypingPayload
import com.revotech.chatserver.business.typing.TypingService
import com.revotech.chatserver.payload.*
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal

/**
 * WebSocket-Only Controller - Theo thiết kế microservices đúng
 * CHỈ handle real-time WebSocket/STOMP messages
 * Không có REST endpoints (@RestController)
 */
@Controller // Không phải @RestController!
class ChatController(
    private val messageService: MessageService,
    private val topicCommentService: TopicCommentService,
    private val groupCommentFileService: GroupCommentFileService,
    private val messageReactionService: MessageReactionService,
    private val threadService: MessageThreadService,
    private val typingService: TypingService,
    private val userPresenceService: UserPresenceService,
    private val messageStatusService: MessageStatusService
) {

    // === MESSAGING ===
    @MessageMapping("/chat/send-message")
    fun sendMessage(@Payload messagePayload: MessagePayload, principal: Principal) =
        messageService.sendMessage(messagePayload, principal)

    // === GROUP TOPICS ===
    @MessageMapping("/group-topic/send-comment")
    fun sendGroupTopicComment(@Payload topicCommentPayload: TopicCommentPayload, principal: Principal) =
        topicCommentService.sendTopicComment(topicCommentPayload, principal)

    // === GROUP FILE COMMENTS ===
    @MessageMapping("/group-file/send-comment")
    fun sendGroupFileComment(@Payload groupCommentFilePayload: GroupCommentFilePayload, principal: Principal) =
        groupCommentFileService.sendComment(groupCommentFilePayload, principal)

    // === REACTIONS ===
    @MessageMapping("/chat/reaction")
    fun handleReaction(@Payload reactionPayload: ReactionPayload, principal: Principal) {
        val userId = principal.name
        if (reactionPayload.emoji.startsWith("remove_")) {
            val emoji = reactionPayload.emoji.removePrefix("remove_")
            messageReactionService.removeReaction(reactionPayload.messageId, emoji, userId)
        } else {
            messageReactionService.addReaction(reactionPayload.messageId, reactionPayload.emoji, userId)
        }
    }

    // === THREADS ===
    @MessageMapping("/chat/thread/reply")
    fun replyToThread(@Payload threadReplyPayload: ThreadReplyPayload, principal: Principal) =
        threadService.replyToThread(threadReplyPayload.threadId, threadReplyPayload, principal)

    // === TYPING INDICATORS ===
    @MessageMapping("/chat/typing")
    fun handleTyping(@Payload typingPayload: TypingPayload, principal: Principal) {
        val userId = principal.name
        typingService.handleTyping(typingPayload.conversationId, userId, typingPayload.isTyping)
    }

    // === MESSAGE STATUS ===
    @MessageMapping("/chat/delivered")
    fun handleMessageDelivered(@Payload payload: MessageStatusPayload, principal: Principal) {
        val userId = principal.name
        messageStatusService.markAsDelivered(payload.messageId, userId)
    }

    @MessageMapping("/chat/read")
    fun handleMessageRead(@Payload payload: MessageStatusPayload, principal: Principal) {
        val userId = principal.name
        messageStatusService.markAsRead(payload.messageId, userId)
    }

    @MessageMapping("/chat/conversation/read")
    fun handleConversationRead(@Payload payload: ConversationReadPayload, principal: Principal) {
        val userId = principal.name
        messageStatusService.markConversationAsRead(payload.conversationId, userId)
    }

    // === PRESENCE ===
    @MessageMapping("/chat/activity")
    fun handleActivity(principal: Principal) {
        userPresenceService.updateUserActivity(principal.name)
    }

    @MessageMapping("/chat/user/online")
    fun handleUserOnline(principal: Principal) {
        val userId = principal.name
        messageStatusService.handleUserOnline(userId)
    }
}