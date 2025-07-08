package com.revotech.chatserver.controller

import com.revotech.chatserver.business.thread.MessageThreadService
import com.revotech.chatserver.payload.ThreadReplyPayload
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/thread")
class ThreadController(
    private val threadService: MessageThreadService
) {

    @PostMapping("/create/{parentMessageId}")
    fun createThread(@PathVariable parentMessageId: String, principal: Principal) =
        threadService.createThread(parentMessageId, principal.name)

    @PostMapping("/{threadId}/reply")
    fun replyToThread(
        @PathVariable threadId: String,
        @RequestBody payload: ThreadReplyPayload,
        principal: Principal
    ) = threadService.replyToThread(threadId, payload, principal)

    @GetMapping("/{threadId}/replies")
    fun getThreadReplies(@PathVariable threadId: String, pageable: Pageable) =
        threadService.getThreadReplies(threadId, pageable)

    @GetMapping("/summary/{parentMessageId}")
    fun getThreadSummary(@PathVariable parentMessageId: String) =
        threadService.getThreadSummary(parentMessageId)

    @GetMapping("/conversation/{conversationId}")
    fun getConversationThreads(@PathVariable conversationId: String) =
        threadService.getConversationThreads(conversationId)
}