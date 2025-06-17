package com.revotech.chatserver.payload

data class ReactionPayload(
    val messageId: String,
    val emoji: String
)