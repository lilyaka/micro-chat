package com.revotech.chatserver.business.thread

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("message_thread")
data class MessageThread(
    @Id
    val id: String?,
    val parentMessageId: String,
    val conversationId: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var lastReplyAt: LocalDateTime = LocalDateTime.now(),
    var replyCount: Int = 0,
    var participants: MutableSet<String> = mutableSetOf()
)
