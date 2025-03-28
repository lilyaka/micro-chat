package com.revotech.chatserver.payload

data class TopicCommentPayload(
    val groupId: String,
    val topicId: String,
    val content: String,
    val replyCommentId: String?,
    val files: MutableList<AttachmentPayload>? = null
)