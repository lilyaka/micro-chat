package com.revotech.chatserver.business.thread

import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.message.Message
import com.revotech.chatserver.business.message.MessageRepository
import com.revotech.chatserver.business.message.MessageType
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.ThreadReplyPayload
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal
import java.time.LocalDateTime

@Service
class MessageThreadService(
    private val threadRepository: MessageThreadRepository,
    private val messageRepository: MessageRepository,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
    private val tenantHelper: TenantHelper // ✅ Added TenantHelper
) {

    // ✅ NEED TENANT CONTEXT - Queries/Creates database
    fun createThread(parentMessageId: String, userId: String, principal: Principal): MessageThread {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val parentMessage = chatService.getMessage(parentMessageId)

            val existingThread = threadRepository.findByParentMessageId(parentMessageId)
            if (existingThread != null) {
                return@changeTenant existingThread
            }

            val thread = MessageThread(
                id = null,
                parentMessageId = parentMessageId,
                conversationId = parentMessage.conversationId,
                participants = mutableSetOf(parentMessage.fromUserId, userId)
            )

            threadRepository.save(thread)
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries/Creates/Updates database
    fun replyToThread(threadId: String, payload: ThreadReplyPayload, principal: Principal): Message {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val thread = threadRepository.findById(threadId).orElseThrow {
                throw ThreadNotFoundException("threadNotFound", "Thread not found")
            }

            val userId = principal.name
            val message = Message.Builder()
                .fromUserId(userId)
                .conversationId(thread.conversationId)
                .content(payload.content)
                .attachments(chatService.convertAttachments(thread.conversationId, payload.files, principal))
                .threadId(threadId)
                .type(MessageType.MESSAGE)
                .build()

            addReplyToThread(threadId, message)
        }
    }

    private fun addReplyToThread(threadId: String, message: Message): Message {
        val thread = threadRepository.findById(threadId).orElseThrow {
            throw ThreadNotFoundException("threadNotFound", "Thread not found")
        }

        thread.lastReplyAt = LocalDateTime.now()
        thread.replyCount++
        thread.participants.add(message.fromUserId)
        threadRepository.save(thread)

        val savedMessage = messageRepository.save(message)

        simpMessagingTemplate.convertAndSend(
            "/chat/thread/$threadId",
            ThreadReplyMessage(savedMessage, thread)
        )

        simpMessagingTemplate.convertAndSend(
            "/chat/user/${thread.conversationId}",
            savedMessage
        )

        return savedMessage
    }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getThreadReplies(threadId: String, pageable: Pageable, principal: Principal): Page<Message> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            messageRepository.findByThreadIdOrderBySentAtAsc(threadId, pageable)
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getThreadSummary(parentMessageId: String, principal: Principal): ThreadSummary? {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val thread = threadRepository.findByParentMessageId(parentMessageId)
            thread?.let {
                val lastReplies = messageRepository.findByThreadIdOrderBySentAtDesc(it.id!!, PageRequest.of(0, 2))
                ThreadSummary(
                    threadId = it.id!!,
                    replyCount = it.replyCount,
                    lastReplyAt = it.lastReplyAt,
                    participants = it.participants.toList(),
                    lastReplies = lastReplies.content
                )
            }
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getConversationThreads(conversationId: String, principal: Principal): List<MessageThread> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            threadRepository.findByConversationId(conversationId)
        }
    }
}

data class ThreadSummary(
    val threadId: String,
    val replyCount: Int,
    val lastReplyAt: LocalDateTime,
    val participants: List<String>,
    val lastReplies: List<Message>
)

data class ThreadReplyMessage(
    val message: Message,
    val thread: MessageThread
)