package com.revotech.chatserver.business.typing

import java.time.LocalDateTime

data class TypingIndicator(
    val userId: String,
    val conversationId: String,
    val userName: String,
    val isTyping: Boolean,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class TypingPayload(
    val conversationId: String,
    val isTyping: Boolean
)

data class TypingUpdateMessage(
    val conversationId: String,
    val typingUsers: List<TypingUser>
)

data class TypingUser(
    val userId: String,
    val userName: String,
    val startTime: LocalDateTime
)