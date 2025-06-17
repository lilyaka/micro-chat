package com.revotech.chatserver.business.message

import com.revotech.chatserver.business.attachment.Attachment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

const val DB_MESSAGE = "chat_message"

@Document(DB_MESSAGE)
data class Message(
    @Id
    val id: String?,
    val fromUserId: String,
    val conversationId: String,
    val content: String?,
    var attachments: MutableList<Attachment>?,
    val sentAt: LocalDateTime,
    var isDeleted: Boolean,
    var readIds: MutableList<String>?,
    var replyMessageId: String?,
    var threadId: String?,
    var type: MessageType,

    // Tối ưu cho status tracking
    var deliveredAt: LocalDateTime? = null,
    var deliveredIds: MutableSet<String> = mutableSetOf(), // Chỉ track trong nhóm nhỏ
    var readAt: LocalDateTime? = null,
    var lastReadAt: LocalDateTime? = null, // Thời gian read gần nhất
    var readCount: Int = 0, // Đếm số người đã đọc (tối ưu cho nhóm lớn)
    var deliveredCount: Int = 0 // Đếm số người đã nhận
) {
    var avatar: String? = ""
    var sender: String? = ""
    var replyMessage: Message? = null

    // Runtime status cho client
    var status: MessageStatus = MessageStatus.SENT
    var deliveryInfo: MessageDeliveryInfo? = null

    class Builder {
        private var fromUserId: String = ""
        private var conversationId: String = ""
        private var content: String? = null
        private var attachments: MutableList<Attachment>? = null
        private var replyMessageId: String? = null
        private var threadId: String? = null
        private var type: MessageType = MessageType.MESSAGE

        fun fromUserId(fromUserId: String) = apply { this.fromUserId = fromUserId }
        fun conversationId(conversationId: String) = apply { this.conversationId = conversationId }
        fun content(content: String?) = apply { this.content = content }
        fun attachments(attachments: MutableList<Attachment>?) =
            apply { this.attachments = attachments?.toMutableList() }

        fun replyMessageId(replyMessageId: String?) = apply { this.replyMessageId = replyMessageId }
        fun threadId(threadId: String?) = apply { this.threadId = threadId }
        fun type(type: MessageType) = apply { this.type = type }

        fun build() = Message(
            null,
            fromUserId,
            conversationId,
            content,
            attachments,
            LocalDateTime.now(),
            false,
            mutableListOf(fromUserId),
            replyMessageId,
            threadId,
            type
        )
    }
}

enum class MessageType {
    MESSAGE,
    ACTION
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

data class MessageDeliveryInfo(
    val totalMembers: Int,
    val deliveredCount: Int,
    val readCount: Int,
    val isGroupChat: Boolean,
    val lastActivity: LocalDateTime?
)