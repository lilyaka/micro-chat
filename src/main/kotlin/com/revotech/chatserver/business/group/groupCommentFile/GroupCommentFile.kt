package com.revotech.chatserver.business.group.groupCommentFile

import com.revotech.chatserver.business.attachment.Attachment
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("ecm_group_comment_file")
data class GroupCommentFile(
        @Id
        var id: String?,
        var fileId: String?,
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
        private var fileId: String = ""
        private var content: String = ""
        private var attachments: MutableList<Attachment>? = null
        private var replyCommentId: String? = null

        fun senderId(senderId: String) = apply { this.senderId = senderId }
        fun fileId(fileId: String) = apply { this.fileId = fileId }
        fun content(content: String) = apply { this.content = content }
        fun attachments(attachments: MutableList<Attachment>?) =
                apply { this.attachments = attachments?.toMutableList() }

        fun replyCommentId(replyCommentId: String?) = apply { this.replyCommentId = replyCommentId }
        fun build() = GroupCommentFile(
                null,
                fileId,
                content,
                senderId,
                LocalDateTime.now(),
                null,
                attachments
        )
    }
}



