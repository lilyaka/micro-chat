package com.revotech.chatserver.business.reaction

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("message_reaction")
data class MessageReaction(
    @Id
    val id: String?,
    val messageId: String,
    val userId: String,
    val emoji: String, // Unicode emoji hoặc custom emoji ID
    val createdAt: LocalDateTime = LocalDateTime.now()
)

