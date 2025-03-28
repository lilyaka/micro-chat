package com.revotech.chatserver.payload

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime


data class ConversationPayload(
    val name: String,
    val isGroup: Boolean,
    val avatar: MultipartFile?,
    val members: MutableList<String>
)

data class ConversationNamePayload(
    val name: String
)

data class MessagePayload(
    val conversationId: String,
    val content: String,
    val replyMessageId: String?,
    val files: MutableList<AttachmentPayload>? = null
)

data class AttachmentPayload(
    val name: String,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val data: String,
    val size: Long
)

data class EcmAttachmentPayload(
    val id: String,
    val name: String,
    val size: Long,
    val path: String,
    val sentAt: LocalDateTime,
    val sender: String?
)

enum class ConversationAction {
    CREATE,
    ADD_MEMBER,
    REMOVE_MEMBER
}

data class ConversationActionMessage(
    val type: ConversationAction,
    val metadata: String
)
