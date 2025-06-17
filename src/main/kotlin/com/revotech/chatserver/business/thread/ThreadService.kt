package com.revotech.chatserver.business.thread

import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.message.Message
import com.revotech.chatserver.business.message.MessageRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MessageThreadService(
    private val threadRepository: MessageThreadRepository,
    private val messageRepository: MessageRepository,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService
) {

    fun createThread(parentMessageId: String, userId: String): MessageThread {
        val parentMessage = chatService.getMessage(parentMessageId)

        // Kiểm tra thread đã tồn tại chưa
        val existingThread = threadRepository.findByParentMessageId(parentMessageId)
        if (existingThread != null) {
            return existingThread
        }

        val thread = MessageThread(
            id = null,
            parentMessageId = parentMessageId,
            conversationId = parentMessage.conversationId,
            participants = mutableSetOf(parentMessage.fromUserId, userId)
        )

        return threadRepository.save(thread)
    }

    fun addReplyToThread(threadId: String, message: Message): Message {
        val thread = threadRepository.findById(threadId).orElseThrow {
            throw ThreadNotFoundException("threadNotFound", "Thread not found")
        }

        // Update thread metadata
        thread.lastReplyAt = LocalDateTime.now()
        thread.replyCount++
        thread.participants.add(message.fromUserId)
        threadRepository.save(thread)

        // Set thread context for message
        message.threadId = threadId
        val savedMessage = messageRepository.save(message)

        // Broadcast to thread subscribers
        simpMessagingTemplate.convertAndSend(
            "/chat/thread/$threadId",
            ThreadReplyMessage(savedMessage, thread)
        )

        return savedMessage
    }

    fun getThreadReplies(threadId: String, pageable: Pageable): Page<Message> {
        return messageRepository.findByThreadIdOrderBySentAtAsc(threadId, pageable)
    }

    fun getThreadSummary(parentMessageId: String): ThreadSummary? {
        val thread = threadRepository.findByParentMessageId(parentMessageId)
        return thread?.let {
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