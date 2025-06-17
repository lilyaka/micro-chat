package com.revotech.chatserver.payload

data class MessageStatusPayload(
    val messageId: String,
    val conversationId: String? = null
)