package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.groupCommentFile.GroupCommentFileService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.topicComment.TopicCommentService
import com.revotech.chatserver.payload.GroupCommentFilePayload
import com.revotech.chatserver.payload.MessagePayload
import com.revotech.chatserver.payload.TopicCommentPayload
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class ChatController(
    private val messageService: MessageService,
    private val topicCommentService: TopicCommentService,
    private val groupCommentFileService: GroupCommentFileService
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
}
