package com.revotech.chatserver.business.topicComment

import com.revotech.chatserver.business.attachment.Attachment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("topic_comment")
data class TopicComment(
    @Id
    var id: String?,
    var topicId: String?,
    var content: String,
    var senderId: String?,
    var sentAt: LocalDateTime?,
    var replyCommentId: String?,
    var attachments: MutableList<Attachment>? = mutableListOf()
) {
    var sender: String? = ""
    var avatar: String? = ""

    class Builder {
        private var senderId: String = ""
        private var topicId: String = ""
        private var content: String = ""
        private var attachments: MutableList<Attachment>? = null
        private var replyCommentId: String? = null

        fun senderId(senderId: String) = apply { this.senderId = senderId }
        fun topicId(topicId: String) = apply { this.topicId = topicId }
        fun content(content: String) = apply { this.content = content }
        fun attachments(attachments: MutableList<Attachment>?) =
            apply { this.attachments = attachments?.toMutableList() }

        fun replyCommentId(replyCommentId: String?) = apply { this.replyCommentId = replyCommentId }
        fun build() = TopicComment(
            null,
            topicId,
            content,
            senderId,
            LocalDateTime.now(),
            null,
            attachments
        )
    }
}



