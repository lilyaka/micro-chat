package com.revotech.chatserver.business.message

import com.revotech.chatserver.business.CHAT_DESTINATION
import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.attachment.Attachment
import com.revotech.chatserver.business.conversation.Conversation
import com.revotech.chatserver.business.user.User
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.MessagePayload
import com.revotech.util.WebUtil
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    private val webUtil: WebUtil
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

    fun markAsReadMessage(conversationId: String): MutableList<Message> {
        val userId = webUtil.getUserId()
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
}
