package com.revotech.chatserver.business.message.status

import com.revotech.chatserver.business.CHAT_DESTINATION
import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.conversation.ConversationRepository
import com.revotech.chatserver.business.message.Message
import com.revotech.chatserver.business.message.MessageRepository
import com.revotech.chatserver.business.message.MessageStatus
import com.revotech.chatserver.business.message.MessageDeliveryInfo
import com.revotech.util.WebUtil
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MessageStatusService(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val mongoTemplate: MongoTemplate,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
    private val webUtil: WebUtil
) {

    companion object {
        private const val GROUP_SIZE_THRESHOLD = 20 // Nhóm > 20 người dùng counter thay vì list
        private const val BATCH_UPDATE_SIZE = 50 // Batch size cho update
    }

    fun markAsDelivered(messageId: String, userId: String) {
        val message = chatService.getMessage(messageId)
        val conversation = chatService.getConversation(message.conversationId)

        if (message.fromUserId == userId) return // Người gửi không cần mark delivered

        val isLargeGroup = conversation.members.size > GROUP_SIZE_THRESHOLD

        if (isLargeGroup) {
            // Nhóm lớn: chỉ increment counter
            incrementDeliveredCount(messageId)
        } else {
            // Nhóm nhỏ: track individual users
            addToDeliveredIds(messageId, userId)
        }

        // Broadcast status update
        broadcastStatusUpdate(message, conversation.members.size, isLargeGroup)
    }

    fun markAsRead(messageId: String, userId: String) {
        val message = chatService.getMessage(messageId)
        val conversation = chatService.getConversation(message.conversationId)

        if (message.fromUserId == userId) return

        val isLargeGroup = conversation.members.size > GROUP_SIZE_THRESHOLD

        if (isLargeGroup) {
            // Nhóm lớn: chỉ increment counter và update timestamp
            incrementReadCount(messageId, userId)
        } else {
            // Nhóm nhỏ: track individual users
            addToReadIds(messageId, userId)
        }

        broadcastStatusUpdate(message, conversation.members.size, isLargeGroup)
    }

    fun markConversationAsRead(conversationId: String, userId: String) {
        // Batch update tất cả unread messages trong conversation
        val unreadMessages = messageRepository.findByConversationIdAndReadIdsNotContains(conversationId, userId)

        // Process in batches để tránh overload
        unreadMessages.chunked(BATCH_UPDATE_SIZE).forEach { batch ->
            batch.forEach { message ->
                markAsRead(message.id!!, userId)
            }
        }
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
            .addToSet("readIds", userId) // Vẫn cần track để tránh duplicate

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
        // Lấy thông tin mới nhất
        val updatedMessage = chatService.getMessage(message.id!!)

        val deliveryInfo = MessageDeliveryInfo(
            totalMembers = totalMembers - 1, // Trừ người gửi
            deliveredCount = if (isLargeGroup) updatedMessage.deliveredCount else updatedMessage.deliveredIds.size,
            readCount = if (isLargeGroup) updatedMessage.readCount else updatedMessage.readIds?.size ?: 0,
            isGroupChat = totalMembers > 2,
            lastActivity = updatedMessage.lastReadAt ?: updatedMessage.deliveredAt
        )

        // Determine status
        val status = when {
            deliveryInfo.readCount > 0 -> MessageStatus.READ
            deliveryInfo.deliveredCount > 0 -> MessageStatus.DELIVERED
            else -> MessageStatus.SENT
        }

        val statusUpdate = MessageStatusUpdate(
            messageId = message.id!!,
            status = status,
            deliveryInfo = deliveryInfo,
            timestamp = LocalDateTime.now()
        )

        // Chỉ gửi cho người gửi tin nhắn
        simpMessagingTemplate.convertAndSendToUser(
            message.fromUserId,
            "$CHAT_DESTINATION/status",
            statusUpdate
        )
    }

    fun getMessageStatus(messageId: String): MessageStatusUpdate? {
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

        return MessageStatusUpdate(
            messageId = messageId,
            status = status,
            deliveryInfo = deliveryInfo,
            timestamp = LocalDateTime.now()
        )
    }

    // Tự động mark delivered khi user online và có tin nhắn mới
    fun handleUserOnline(userId: String) {
        // Tìm các conversation mà user tham gia
        val conversations = conversationRepository.findUserConversation(userId)

        conversations.forEach { conversation ->
            // Mark delivered cho các tin nhắn chưa delivered gần đây (trong 24h)
            val recentMessages = messageRepository.findRecentUndeliveredMessages(
                conversation.id!!,
                userId,
                LocalDateTime.now().minusHours(24)
            )

            recentMessages.forEach { message ->
                markAsDelivered(message.id!!, userId)
            }
        }
    }
}

data class MessageStatusUpdate(
    val messageId: String,
    val status: MessageStatus,
    val deliveryInfo: MessageDeliveryInfo,
    val timestamp: LocalDateTime
)