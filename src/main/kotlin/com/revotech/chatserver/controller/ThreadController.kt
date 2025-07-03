package com.revotech.chatserver.controller

import com.revotech.chatserver.business.thread.MessageThreadService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.ThreadReplyPayload
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/thread")
class ThreadController(
    private val threadService: MessageThreadService,
    private val tenantHelper: TenantHelper
) {

    @PostMapping("/create/{parentMessageId}")
    fun createThread(
        @PathVariable parentMessageId: String,
        principal: Principal
    ) = threadService.createThread(parentMessageId, principal.name, principal)

    @PostMapping("/{threadId}/reply")
    fun replyToThread(
        @PathVariable threadId: String,
        @RequestBody payload: ThreadReplyPayload,
        principal: Principal
    ) = threadService.replyToThread(threadId, payload, principal)

    @GetMapping("/{threadId}/replies")
    fun getThreadReplies(
        @PathVariable threadId: String,
        pageable: Pageable,
        principal: Principal
    ) = threadService.getThreadReplies(threadId, pageable, principal)

    @GetMapping("/summary/{parentMessageId}")
    fun getThreadSummary(
        @PathVariable parentMessageId: String,
        principal: Principal
    ) = threadService.getThreadSummary(parentMessageId, principal)

    @GetMapping("/conversation/{conversationId}")
    fun getConversationThreads(
        @PathVariable conversationId: String,
        principal: Principal
    ) = threadService.getConversationThreads(conversationId, principal)
}