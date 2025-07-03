package com.revotech.chatserver.controller

import com.revotech.chatserver.business.ExportHistoryService
import com.revotech.chatserver.business.attachment.Attachment
import com.revotech.chatserver.business.message.MessageContextResponse
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.helper.TenantHelper
import org.springframework.core.io.Resource
import org.springframework.data.domain.Pageable
import org.springframework.http.ContentDisposition
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.security.Principal

@RestController
@RequestMapping("/history")
class HistoryController(
    private val messageService: MessageService,
    private val exportHistoryService: ExportHistoryService,
    private val tenantHelper: TenantHelper
) {
    @GetMapping
    fun conversationHistory(
        conversationId: String,
        pageable: Pageable,
        principal: Principal
    ) = tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
        messageService.getHistories(conversationId, pageable)
    }

    @GetMapping("/attachment/{attachmentId}")
    fun getAttachment(
        @PathVariable attachmentId: String,
        principal: Principal
    ): Attachment? = tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
        messageService.getAttachment(attachmentId, principal)
    }

    @PostMapping("/export")
    fun exportHistory(
        conversationId: String,
        principal: Principal
    ): ResponseEntity<Resource> = tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
        val resource = exportHistoryService.exportHistory(conversationId, principal)
        responseEntityResource(resource)
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

    @GetMapping("/jump-to-message/{messageId}")
    fun jumpToMessage(
        @PathVariable messageId: String,
        @RequestParam conversationId: String,
        @RequestParam(defaultValue = "20") pageSize: Int,
        principal: Principal
    ): MessageContextResponse = tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
        messageService.getMessageContext(conversationId, messageId, pageSize, principal)
    }

    @GetMapping("/message-context/{messageId}")
    fun getMessageContext(
        @PathVariable messageId: String,
        @RequestParam conversationId: String,
        @RequestParam(defaultValue = "10") beforeCount: Int,
        @RequestParam(defaultValue = "10") afterCount: Int,
        principal: Principal
    ): MessageContextResponse = tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
        messageService.getMessageContextWithOffset(conversationId, messageId, beforeCount, afterCount, principal)
    }
}