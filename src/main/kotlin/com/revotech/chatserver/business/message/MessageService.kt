package com.revotech.chatserver.business.message

import com.revotech.chatserver.business.CHAT_DESTINATION
import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.attachment.Attachment
import com.revotech.chatserver.business.conversation.Conversation
import com.revotech.chatserver.business.presence.UserPresenceService
import com.revotech.chatserver.business.typing.TypingService
import com.revotech.chatserver.business.user.User
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.MessagePayload
import com.revotech.util.WebUtil
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val tenantHelper: TenantHelper,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val userService: UserService,
    private val chatService: ChatService,
    private val webUtil: WebUtil,
    private val userPresenceService: UserPresenceService,
    private val typingService: TypingService
) {
    fun sendMessage(messagePayload: MessagePayload, principal: Principal) {
        val userId = principal.name

        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            messagePayload.run {
                val conversation = chatService.getConversation(conversationId)

                val message = Message.Builder()
                    .fromUserId(userId)
                    .conversationId(conversationId)
                    .content(content)
                    .attachments(chatService.convertAttachments(conversation.id as String, files, principal))
                    .replyMessageId(replyMessageId)
                    .type(MessageType.MESSAGE)
                    .build()

                // Track user activity khi gửi message
                userPresenceService.updateUserActivity(userId)

                // ✅ CLEAR TYPING TRONG TENANT CONTEXT (GỌI INTERNAL METHOD)
                typingService.clearTyping(conversationId, userId)

                simpMessagingTemplate.convertAndSend(
                    "${CHAT_DESTINATION}/${if (conversation.isGroup) "group" else "user"}/${messagePayload.conversationId}",
                    getSentMessage(messageRepository.save(message))
                )

                conversation.lastMessage = message
                conversation.totalAttachment = files?.let { conversation.totalAttachment?.plus(it.size) }

                chatService.saveConversation(conversation)
            }
        }
    }

    private fun getSentMessage(message: Message): Message {
        val user = userService.getUser(message.fromUserId)
        message.sender = user?.fullName ?: ""

        if (message.replyMessageId != null) {
            message.replyMessage = chatService.getMessage(message.replyMessageId!!)
            message.replyMessage!!.sender = userService.getUser(message.replyMessage!!.fromUserId)?.fullName
        }

        return message
    }

    fun markAsReadMessage(conversationId: String, userId: String): MutableList<Message> {
        // Track activity khi đọc tin nhắn
        userPresenceService.updateUserActivity(userId)

        return readMessages(userId) {
            findUnreadMessages(userId, conversationId)
        }
    }

    private fun readMessages(userId: String, findFunc: () -> List<Message>): MutableList<Message> {
        val chats = findFunc()
            .map {
                it.readIds?.add(userId)
                it
            }
        return messageRepository.saveAll(chats)
    }

    private fun findUnreadMessages(userId: String, conversationId: String): List<Message> {
        return messageRepository.findByConversationIdAndReadIdsNotContains(conversationId, userId)
    }

    fun searchMessageContent(keyword: String) = messageRepository.findByContent(keyword, MessageType.MESSAGE)

    fun searchMessageAttachments(keyword: String) =
        messageRepository.findByAttachmentsName(keyword, MessageType.MESSAGE)

    fun searchMessage(keyword: String): MutableList<Conversation> {
        val contentMessages = searchMessageContent(keyword)
        val attachmentMessages = searchMessageAttachments(keyword)

        val mapUser = HashMap<String, User?>()
        val mapConversation = HashMap<String, Conversation>()

        return contentMessages.plus(attachmentMessages).sortedWith(compareBy({ it.conversationId }, { it.sentAt }))
            .map {
                val conversation = if (mapConversation.containsKey(it.conversationId)) {
                    mapConversation[it.conversationId] as Conversation
                } else {
                    chatService.getConversation(it.conversationId)
                }
                getMessageInfo(mapUser, it)
                conversation.lastMessage = it

                conversation
            }.toMutableList()
    }

    private fun getMessageInfo(mapUser: HashMap<String, User?>, message: Message) {
        val fromId = message.fromUserId
        if (!mapUser.containsKey(fromId)) {
            val user = userService.getUser(fromId)
            mapUser[fromId] = user
        }
        message.avatar = mapUser[fromId]?.avatar ?: ""
        message.sender = mapUser[fromId]?.fullName ?: ""
    }

    fun getAllHistory(conversationId: String) = messageRepository.findByConversationId(conversationId)

    fun getAttachment(attachmentId: String): Attachment? =
        messageRepository.findAttachmentById(attachmentId).orElse(null)

    /**
     * Tìm page chứa target message và return context xung quanh
     */
    fun getMessageContext(conversationId: String, messageId: String, pageSize: Int = 20): MessageContextResponse {
        // 1. Lấy target message
        val targetMessage = chatService.getMessage(messageId)

        // 2. Đếm số messages newer than target
        val newerMessagesCount = messageRepository.countMessagesNewerThan(
            conversationId,
            targetMessage.sentAt
        )

        // 3. Tính page number chứa target message
        val targetPageNumber = (newerMessagesCount / pageSize).toInt()

        // 4. Lấy messages từ page đó
        val pageable = PageRequest.of(targetPageNumber, pageSize, Sort.by(Sort.Direction.DESC, "sentAt"))
        val messagesPage = getHistories(conversationId, pageable)

        // 5. Tìm index của target message trong page
        val targetIndex = messagesPage.content.indexOfFirst { it.id == messageId }

        return MessageContextResponse(
            messages = messagesPage.content,
            targetMessageId = messageId,
            targetIndex = targetIndex,
            pageNumber = targetPageNumber,
            totalPages = messagesPage.totalPages,
            totalElements = messagesPage.totalElements,
            hasNext = messagesPage.hasNext(),
            hasPrevious = messagesPage.hasPrevious()
        )
    }

    /**
     * Alternative: Lấy context xung quanh message với offset
     */
    fun getMessageContextWithOffset(
        conversationId: String,
        messageId: String,
        beforeCount: Int = 10,
        afterCount: Int = 10
    ): MessageContextResponse {

        val targetMessage = chatService.getMessage(messageId)

        // Lấy messages before target (newer messages)
        val beforePageable = PageRequest.of(0, beforeCount, Sort.by(Sort.Direction.ASC, "sentAt"))
        val messagesAfter = messageRepository.findByConversationIdAndSentAtGreaterThan(
            conversationId, targetMessage.sentAt, beforePageable
        ).content.reversed() // Reverse để có thứ tự desc

        // Lấy messages after target (older messages)
        val afterPageable = PageRequest.of(0, afterCount, Sort.by(Sort.Direction.DESC, "sentAt"))
        val messagesBefore = messageRepository.findByConversationIdAndSentAtLessThan(
            conversationId, targetMessage.sentAt, afterPageable
        ).content

        // Combine: newer + target + older
        val allMessages = mutableListOf<Message>().apply {
            addAll(messagesAfter)
            add(targetMessage)
            addAll(messagesBefore)
        }

        // Populate user info
        val mapUser = HashMap<String, User?>()
        allMessages.forEach { message ->
            getMessageInfo(mapUser, message)

            if (message.replyMessageId != null) {
                val replyMessage = allMessages.find { it.id == message.replyMessageId }
                    ?: chatService.getMessage(message.replyMessageId!!)
                message.replyMessage = replyMessage
            }
        }

        return MessageContextResponse(
            messages = allMessages,
            targetMessageId = messageId,
            targetIndex = messagesAfter.size, // Index của target message
            pageNumber = -1, // Không áp dụng cho context mode
            totalPages = -1,
            totalElements = allMessages.size.toLong(),
            hasNext = false,
            hasPrevious = false
        )
    }

    fun getHistories(conversationId: String, pageable: Pageable): Page<Message> {
        val mapUser = HashMap<String, User?>()
        val histories = messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable)
        return histories.map {
            getMessageInfo(mapUser, it)

            if (it.replyMessageId != null) {
                var existMessage = histories.find { mess -> mess.id == it.replyMessageId }
                if (existMessage == null) {
                    existMessage = chatService.getMessage(it.replyMessageId!!)
                }
                it.replyMessage = existMessage
            }

            it
        }
    }
}

data class MessageContextResponse(
    val messages: List<Message>,
    val targetMessageId: String,
    val targetIndex: Int, // Index của target message trong list
    val pageNumber: Int,
    val totalPages: Int,
    val totalElements: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)