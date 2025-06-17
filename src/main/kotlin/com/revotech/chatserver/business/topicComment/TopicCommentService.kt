package com.revotech.chatserver.business.topicComment

import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.GROUP_TOPIC_DESTINATION
import com.revotech.chatserver.business.topic.TopicRepository
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.TopicCommentPayload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal
import java.time.LocalDateTime

@Service
class TopicCommentService(
    private val topicCommentRepository: TopicCommentRepository,
    private val userService: UserService,
    private val topicRepository: TopicRepository,
    private val tenantHelper: TenantHelper,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
) {

    fun sendTopicComment(topicCommentPayload: TopicCommentPayload, principal: Principal) {
        val userId = principal.name

        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            topicCommentPayload.run {

                val comment = TopicComment.Builder()
                    .senderId(userId)
                    .topicId(topicCommentPayload.topicId)
                    .content(content)
                    .attachments(chatService.convertAttachments(topicCommentPayload.topicId, files, principal))
                    .replyCommentId(topicCommentPayload.replyCommentId)
                    .build()
                simpMessagingTemplate.convertAndSend(
                    "$GROUP_TOPIC_DESTINATION/${topicCommentPayload.groupId}/${topicCommentPayload.topicId}",
                    getSentCommentInfo(topicCommentRepository.save(comment))
                )
            }
        }
    }

    private fun getSentCommentInfo(topicComment: TopicComment): TopicComment {
        //N+1 query khi load nhiều comments
        //Fix: Sử dụng batch loading hoặc join query
        userService.getUser(topicComment.senderId!!)?.run {
            topicComment.sender = fullName
            topicComment.avatar = avatar
        }

        topicRepository.findById(topicComment.topicId!!).ifPresent {
            it.lastSentAt = LocalDateTime.now()
            topicRepository.save(it)
        }

        return topicComment
    }
}
