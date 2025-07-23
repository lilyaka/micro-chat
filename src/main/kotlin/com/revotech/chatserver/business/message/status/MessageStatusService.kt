package com.revotech.chatserver.business.message.status

import com.revotech.chatserver.business.CHAT_DESTINATION
import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.conversation.ConversationRepository
import com.revotech.chatserver.business.message.Message
import com.revotech.chatserver.business.message.MessageRepository
import com.revotech.chatserver.business.message.MessageStatus
import com.revotech.chatserver.business.message.MessageDeliveryInfo
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.util.WebUtil
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal
import java.time.LocalDateTime

@Service
class MessageStatusService(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val mongoTemplate: MongoTemplate,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
    private val tenantHelper: TenantHelper
) {

    companion object {
        private const val GROUP_SIZE_THRESHOLD = 20
        private const val BATCH_UPDATE_SIZE = 50
    }

    fun markAsDelivered(messageId: String, userId: String, principal: Principal) {
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val message = chatService.getMessage(messageId)
            val conversation = chatService.getConversation(message.conversationId)

            if (message.fromUserId == userId) return@changeTenant

            val isLargeGroup = conversation.members.size > GROUP_SIZE_THRESHOLD

            if (isLargeGroup) {
                incrementDeliveredCount(messageId)
            } else {
                addToDeliveredIds(messageId, userId)
            }

            broadcastStatusUpdate(message, conversation.members.size, isLargeGroup)
        }
    }

    fun markAsRead(messageId: String, userId: String, principal: Principal) {
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val message = chatService.getMessage(messageId)
            val conversation = chatService.getConversation(message.conversationId)

            if (message.fromUserId == userId) return@changeTenant

            val isLargeGroup = conversation.members.size > GROUP_SIZE_THRESHOLD

            if (isLargeGroup) {
                incrementReadCount(messageId, userId)
            } else {
                addToReadIds(messageId, userId)
            }

            broadcastStatusUpdate(message, conversation.members.size, isLargeGroup)
        }
    }

    fun markConversationAsRead(conversationId: String, userId: String, principal: Principal) {
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val unreadMessages = messageRepository.findByConversationIdAndReadIdsNotContains(conversationId, userId)

            unreadMessages.chunked(BATCH_UPDATE_SIZE).forEach { batch ->
                batch.forEach { message ->
                    markAsReadInternal(message.id!!, userId)
                }
            }
        }
    }

    private fun markAsReadInternal(messageId: String, userId: String) {
        val message = chatService.getMessage(messageId)
        val conversation = chatService.getConversation(message.conversationId)

        if (message.fromUserId == userId) return

        val isLargeGroup = conversation.members.size > GROUP_SIZE_THRESHOLD

        if (isLargeGroup) {
            incrementReadCount(messageId, userId)
        } else {
            addToReadIds(messageId, userId)
        }

        broadcastStatusUpdate(message, conversation.members.size, isLargeGroup)
    }

    private fun incrementDeliveredCount(messageId: String) {
        val query = Query.query(Criteria.where("id").`is`(messageId))
        val update = Update()
            .inc("deliveredCount", 1)
            .set("deliveredAt", LocalDateTime.now())

        mongoTemplate.updateFirst(query, update, Message::class.java)
    }

    private fun incrementReadCount(messageId: String, userId: String) {
        val query = Query.query(Criteria.where("id").`is`(messageId))
        val update = Update()
            .inc("readCount", 1)
            .set("lastReadAt", LocalDateTime.now())
            .addToSet("readIds", userId)

        mongoTemplate.updateFirst(query, update, Message::class.java)
    }

    private fun addToDeliveredIds(messageId: String, userId: String) {
        val query = Query.query(Criteria.where("id").`is`(messageId))
        val update = Update()
            .addToSet("deliveredIds", userId)
            .set("deliveredAt", LocalDateTime.now())

        mongoTemplate.updateFirst(query, update, Message::class.java)
    }

    private fun addToReadIds(messageId: String, userId: String) {
        val query = Query.query(Criteria.where("id").`is`(messageId))
        val update = Update()
            .addToSet("readIds", userId)
            .set("readAt", LocalDateTime.now())
            .set("lastReadAt", LocalDateTime.now())

        mongoTemplate.updateFirst(query, update, Message::class.java)
    }

    private fun broadcastStatusUpdate(message: Message, totalMembers: Int, isLargeGroup: Boolean) {
        val updatedMessage = chatService.getMessage(message.id!!)

        val deliveryInfo = MessageDeliveryInfo(
            totalMembers = totalMembers - 1,
            deliveredCount = if (isLargeGroup) updatedMessage.deliveredCount else updatedMessage.deliveredIds.size,
            readCount = if (isLargeGroup) updatedMessage.readCount else updatedMessage.readIds?.size ?: 0,
            isGroupChat = totalMembers > 2,
            lastActivity = updatedMessage.lastReadAt ?: updatedMessage.deliveredAt
        )

        val status = when {
            deliveryInfo.readCount > 0 -> MessageStatus.READ
            deliveryInfo.deliveredCount > 0 -> MessageStatus.DELIVERED
            else -> MessageStatus.SENT
        }

        val statusUpdate = MessageStatusUpdate(
            messageId = message.id,
            status = status,
            deliveryInfo = deliveryInfo,
            timestamp = LocalDateTime.now()
        )

        simpMessagingTemplate.convertAndSendToUser(
            message.fromUserId,
            "$CHAT_DESTINATION/status",
            statusUpdate
        )
    }

    fun getMessageStatus(messageId: String, principal: Principal): MessageStatusUpdate? {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val message = chatService.getMessage(messageId)
            val conversation = chatService.getConversation(message.conversationId)
            val isLargeGroup = conversation.members.size > GROUP_SIZE_THRESHOLD

            val deliveryInfo = MessageDeliveryInfo(
                totalMembers = conversation.members.size - 1,
                deliveredCount = if (isLargeGroup) message.deliveredCount else message.deliveredIds.size,
                readCount = if (isLargeGroup) message.readCount else message.readIds?.size ?: 0,
                isGroupChat = conversation.members.size > 2,
                lastActivity = message.lastReadAt ?: message.deliveredAt
            )

            val status = when {
                deliveryInfo.readCount > 0 -> MessageStatus.READ
                deliveryInfo.deliveredCount > 0 -> MessageStatus.DELIVERED
                else -> MessageStatus.SENT
            }

            MessageStatusUpdate(
                messageId = messageId,
                status = status,
                deliveryInfo = deliveryInfo,
                timestamp = LocalDateTime.now()
            )
        }
    }

    fun handleUserOnline(userId: String, principal: Principal) {
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val conversations = conversationRepository.findUserConversation(userId)
            val cutoffTime = LocalDateTime.now().minusHours(24)

            conversations.forEach { conversation ->
                val recentMessages = messageRepository.findRecentUndeliveredMessages(
                    conversation.id!!,
                    userId,
                    cutoffTime
                )

                recentMessages.forEach { message ->
                    markAsDeliveredInternal(message.id!!, userId)
                }
            }
        }
    }

    private fun markAsDeliveredInternal(messageId: String, userId: String) {
        val message = chatService.getMessage(messageId)
        val conversation = chatService.getConversation(message.conversationId)

        if (message.fromUserId == userId) return

        val isLargeGroup = conversation.members.size > GROUP_SIZE_THRESHOLD

        if (isLargeGroup) {
            incrementDeliveredCount(messageId)
        } else {
            addToDeliveredIds(messageId, userId)
        }

        broadcastStatusUpdate(message, conversation.members.size, isLargeGroup)
    }
}

data class MessageStatusUpdate(
    val messageId: String,
    val status: MessageStatus,
    val deliveryInfo: MessageDeliveryInfo,
    val timestamp: LocalDateTime
)