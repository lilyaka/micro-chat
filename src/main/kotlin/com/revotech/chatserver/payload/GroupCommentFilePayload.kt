package com.revotech.chatserver.payload

data class GroupCommentFilePayload(
    val fileId: String,
    val content: String,
    val replyCommentId: String?,
    val files: MutableList<AttachmentPayload>? = null
)