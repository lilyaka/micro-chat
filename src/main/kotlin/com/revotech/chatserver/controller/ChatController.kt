package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.groupCommentFile.GroupCommentFileService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.presence.UserPresenceService
import com.revotech.chatserver.business.reaction.MessageReactionService
import com.revotech.chatserver.business.thread.MessageThreadService
import com.revotech.chatserver.business.topicComment.TopicCommentService
import com.revotech.chatserver.business.typing.TypingPayload
import com.revotech.chatserver.business.typing.TypingService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.*
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.security.authentication.AbstractAuthenticationToken
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
    private val tenantHelper: TenantHelper,
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
    fun handleReaction(@Payload reactionPayload: ReactionPayload, principal: Principal) {
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val userId = principal.name

            if (reactionPayload.emoji.startsWith("remove_")) {
                val emoji = reactionPayload.emoji.removePrefix("remove_")
                messageReactionService.removeReaction(reactionPayload.messageId, emoji, userId, principal)
            } else {
                messageReactionService.addReaction(reactionPayload.messageId, reactionPayload.emoji, userId, principal)
            }
        }
    }


    @MessageMapping("/chat/thread/reply")
    fun replyToThread(threadReplyPayload: ThreadReplyPayload, principal: Principal) =
        threadService.replyToThread(threadReplyPayload.threadId, threadReplyPayload, principal)

    @MessageMapping("/chat/typing")
    fun handleTyping(typingPayload: TypingPayload, principal: Principal) {
        // ✅ THÊM TENANT CONTEXT
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            typingService.handleTyping(typingPayload, principal)
        }
    }

    @MessageMapping("/chat/activity")
    fun handleActivity(principal: Principal) {
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            userPresenceService.updateUserActivity(principal.name)
        }
    }
}