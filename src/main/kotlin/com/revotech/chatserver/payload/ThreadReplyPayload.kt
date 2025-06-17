package com.revotech.chatserver.payload

data class ThreadReplyPayload(
    val threadId: String, // Cho WebSocket message
    val content: String,
    val files: MutableList<AttachmentPayload>? = null
)