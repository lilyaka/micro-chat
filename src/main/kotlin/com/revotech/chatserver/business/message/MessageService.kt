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

                userPresenceService.updateUserActivity(userId)
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

    // ✅ NEED TENANT CONTEXT - Queries/Updates database
    fun markAsReadMessage(conversationId: String, userId: String, principal: Principal): MutableList<Message> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            userPresenceService.updateUserActivity(userId)
            readMessages(userId) {
                findUnreadMessages(userId, conversationId)
            }
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

    // ✅ NEED TENANT CONTEXT - Queries database
    fun searchMessageContent(keyword: String, principal: Principal) =
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            messageRepository.findByContent(keyword, MessageType.MESSAGE)
        }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun searchMessageAttachments(keyword: String, principal: Principal) =
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            messageRepository.findByAttachmentsName(keyword, MessageType.MESSAGE)
        }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun searchMessage(keyword: String, principal: Principal): MutableList<Conversation> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val contentMessages = searchMessageContent(keyword, principal)
            val attachmentMessages = searchMessageAttachments(keyword, principal)

            val mapUser = HashMap<String, User?>()
            val mapConversation = HashMap<String, Conversation>()

            contentMessages.plus(attachmentMessages).sortedWith(compareBy({ it.conversationId }, { it.sentAt }))
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

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getAllHistory(conversationId: String, principal: Principal) =
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            messageRepository.findByConversationId(conversationId)
        }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getAttachment(attachmentId: String, principal: Principal): Attachment? =
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            messageRepository.findAttachmentById(attachmentId).orElse(null)
        }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getMessageContext(conversationId: String, messageId: String, pageSize: Int = 20, principal: Principal): MessageContextResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val targetMessage = chatService.getMessage(messageId)

            val newerMessagesCount = messageRepository.countMessagesNewerThan(
                conversationId,
                targetMessage.sentAt
            )

            val targetPageNumber = (newerMessagesCount / pageSize).toInt()

            val pageable = PageRequest.of(targetPageNumber, pageSize, Sort.by(Sort.Direction.DESC, "sentAt"))
            val messagesPage = getHistories(conversationId, pageable)

            val targetIndex = messagesPage.content.indexOfFirst { it.id == messageId }

            MessageContextResponse(
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
    }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getMessageContextWithOffset(
        conversationId: String,
        messageId: String,
        beforeCount: Int = 10,
        afterCount: Int = 10,
        principal: Principal
    ): MessageContextResponse {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val targetMessage = chatService.getMessage(messageId)

            val beforePageable = PageRequest.of(0, beforeCount, Sort.by(Sort.Direction.ASC, "sentAt"))
            val messagesAfter = messageRepository.findByConversationIdAndSentAtGreaterThan(
                conversationId, targetMessage.sentAt, beforePageable
            ).content.reversed()

            val afterPageable = PageRequest.of(0, afterCount, Sort.by(Sort.Direction.DESC, "sentAt"))
            val messagesBefore = messageRepository.findByConversationIdAndSentAtLessThan(
                conversationId, targetMessage.sentAt, afterPageable
            ).content

            val allMessages = mutableListOf<Message>().apply {
                addAll(messagesAfter)
                add(targetMessage)
                addAll(messagesBefore)
            }

            val mapUser = HashMap<String, User?>()
            allMessages.forEach { message ->
                getMessageInfo(mapUser, message)

                if (message.replyMessageId != null) {
                    val replyMessage = allMessages.find { it.id == message.replyMessageId }
                        ?: chatService.getMessage(message.replyMessageId!!)
                    message.replyMessage = replyMessage
                }
            }

            MessageContextResponse(
                messages = allMessages,
                targetMessageId = messageId,
                targetIndex = messagesAfter.size,
                pageNumber = -1,
                totalPages = -1,
                totalElements = allMessages.size.toLong(),
                hasNext = false,
                hasPrevious = false
            )
        }
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
    val targetIndex: Int,
    val pageNumber: Int,
    val totalPages: Int,
    val totalElements: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)