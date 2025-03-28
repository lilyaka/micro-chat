package com.revotech.chatserver.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient("file-service")
interface FileServiceClient {
    @PostMapping("/chat/upload-attachment")
    fun uploadChatAttachment(
        @RequestHeader headers: Map<String, Any>,
        @RequestBody chatAttachmentPayload: ChatAttachmentPayload
    ): String

    @DeleteMapping("/chat/delete-conversation")
    fun deleteChatConversation(
        @RequestHeader headers: Map<String, Any>,
        @RequestParam path: String
    )
}

class ChatAttachmentPayload(
    val conversationId: String,
    val attachmentId: String,
    val fileName: String,
    val base64: String
)