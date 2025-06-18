package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.groupCommentFile.GroupCommentFileService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.presence.UserPresenceService
import com.revotech.chatserver.business.reaction.MessageReactionService
import com.revotech.chatserver.business.thread.MessageThreadService
import com.revotech.chatserver.business.topicComment.TopicCommentService
import com.revotech.chatserver.business.typing.TypingPayload
import com.revotech.chatserver.business.typing.TypingService
import com.revotech.chatserver.payload.*
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatController(
    private val messageService: MessageService,
    private val topicCommentService: TopicCommentService,
    private val groupCommentFileService: GroupCommentFileService,
    private val messageReactionService: MessageReactionService,
    private val threadService: MessageThreadService,
    private val typingService: TypingService,
    private val userPresenceService: UserPresenceService,
) {
    @MessageMapping("/chat/send-message")
    fun sendMessage(messagePayload: MessagePayload, principal: Principal) =
        messageService.sendMessage(messagePayload, principal)

    @MessageMapping("/group-topic/send-comment")
    fun sendGroupTopicComment(topicCommentPayload: TopicCommentPayload, principal: Principal) =
        topicCommentService.sendTopicComment(topicCommentPayload, principal)

    @MessageMapping("/group-comment-file/send-comment")
    fun sendGroupCommentFile(groupCommentFilePayload: GroupCommentFilePayload, principal: Principal) =
            groupCommentFileService.sendComment(groupCommentFilePayload, principal)

    @MessageMapping("/chat/reaction")
    fun handleReaction(reactionPayload: ReactionPayload, principal: Principal) {
        val userId = principal.name
        if (reactionPayload.emoji.startsWith("remove_")) {
            val emoji = reactionPayload.emoji.removePrefix("remove_")
            messageReactionService.removeReaction(reactionPayload.messageId, emoji, userId)
        } else {
            messageReactionService.addReaction(reactionPayload.messageId, reactionPayload.emoji, userId)
        }
    }

    @MessageMapping("/chat/thread/reply")
    fun replyToThread(threadReplyPayload: ThreadReplyPayload, principal: Principal) =
        threadService.replyToThread(threadReplyPayload.threadId, threadReplyPayload, principal)

    @MessageMapping("/chat/typing")
    fun handleTyping(typingPayload: TypingPayload, principal: Principal) {
        val userId = principal.name
        typingService.handleTyping(typingPayload.conversationId, userId, typingPayload.isTyping)
    }

    @MessageMapping("/chat/activity")
    fun handleActivity(principal: Principal) {
        userPresenceService.updateUserActivity(principal.name)
    }
}
