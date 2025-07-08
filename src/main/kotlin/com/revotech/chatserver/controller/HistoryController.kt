package com.revotech.chatserver.controller

import com.revotech.chatserver.business.ExportHistoryService
import com.revotech.chatserver.business.attachment.Attachment
import com.revotech.chatserver.business.message.MessageContextResponse
import com.revotech.chatserver.business.message.MessageService
import org.springframework.core.io.Resource
import org.springframework.data.domain.Pageable
import org.springframework.http.ContentDisposition
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder

@RestController
@RequestMapping("/history")
class HistoryController(
    private val messageService: MessageService,
    private val exportHistoryService: ExportHistoryService
) {
    @GetMapping
    fun conversationHistory(conversationId: String, pageable: Pageable) =
        messageService.getHistories(conversationId, pageable)

    @GetMapping("/attachment/{attachmentId}")
    fun getAttachment(@PathVariable attachmentId: String): Attachment? = messageService.getAttachment(attachmentId)

    @PostMapping("/export")
    fun exportHistory(conversationId: String): ResponseEntity<Resource> {
        val resource = exportHistoryService.exportHistory(conversationId)
        return responseEntityResource(resource)
    }

    private fun responseEntityResource(resource: Resource, filename: String? = "compress.zip") = ResponseEntity.ok()
        .headers {
            it.contentType = MediaType.APPLICATION_OCTET_STREAM
            val contentDisposition = ContentDisposition.attachment()
                .filename(
                    URLEncoder.encode(filename ?: resource.filename, Charsets.UTF_8)
                        .replace("+", "%20")
                )
                .build()
            it.contentDisposition = contentDisposition
        }
        .body(resource)

    /**
     * Jump to specific message và lấy context xung quanh
     */
    @GetMapping("/jump-to-message/{messageId}")
    fun jumpToMessage(
        @PathVariable messageId: String,
        @RequestParam conversationId: String,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): MessageContextResponse {
        return messageService.getMessageContext(conversationId, messageId, pageSize)
    }

    /**
     * Alternative: Lấy context với offset cố định
     */
    @GetMapping("/message-context/{messageId}")
    fun getMessageContext(
        @PathVariable messageId: String,
        @RequestParam conversationId: String,
        @RequestParam(defaultValue = "10") beforeCount: Int,
        @RequestParam(defaultValue = "10") afterCount: Int
    ): MessageContextResponse {
        return messageService.getMessageContextWithOffset(conversationId, messageId, beforeCount, afterCount)
    }
}
