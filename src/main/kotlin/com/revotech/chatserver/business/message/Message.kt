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
    var type: MessageType
) {
    var avatar: String? = ""
    var sender: String? = ""
    var replyMessage: Message? = null

    class Builder {
        private var fromUserId: String = ""
        private var conversationId: String = ""
        private var content: String? = null
        private var attachments: MutableList<Attachment>? = null
        private var replyMessageId: String? = null
        private var type: MessageType = MessageType.MESSAGE

        fun fromUserId(fromUserId: String) = apply { this.fromUserId = fromUserId }
        fun conversationId(conversationId: String) = apply { this.conversationId = conversationId }
        fun content(content: String?) = apply { this.content = content }
        fun attachments(attachments: MutableList<Attachment>?) =
            apply { this.attachments = attachments?.toMutableList() }

        fun replyMessageId(replyMessageId: String?) = apply { this.replyMessageId = replyMessageId }
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
            type
        )
    }
}

enum class MessageType {
    MESSAGE,
    ACTION
}

