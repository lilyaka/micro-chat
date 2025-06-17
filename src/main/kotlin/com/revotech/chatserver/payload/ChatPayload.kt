package com.revotech.chatserver.payload

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime


data class ConversationPayload(
    val name: String = "", // Optional cho 1-on-1, required cho nhóm
    @JsonProperty("isGroup") // Deprecated, sẽ tự động detect
    val isGroup: Boolean? = null, // Backward compatibility
    val avatar: MultipartFile? = null,
    val members: MutableList<String> // Chỉ cần list user IDs
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

/**
 * Quick conversation payloads for common scenarios
 */
data class QuickChatPayload(
    val userIds: List<String>,
    val groupName: String? = null // Only needed if >2 users
)

data class MultiUserChatPayload(
    val userIds: List<String>,
    val chatName: String
)