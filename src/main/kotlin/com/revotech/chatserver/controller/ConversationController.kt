package com.revotech.chatserver.controller

import com.revotech.chatserver.business.conversation.ConversationService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.*
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/conversation")
class ConversationController(
    private val conversationService: ConversationService,
    private val messageService: MessageService,
    private val userService: UserService,
    private val tenantHelper: TenantHelper
) {

    @GetMapping("")
    fun getConversations(principal: Principal) =
        conversationService.getUserConversations(principal.name, principal)

    @PostMapping("/create")
    fun createConversation(
        @ModelAttribute conversationPayload: ConversationPayload,
        principal: Principal
    ) = conversationService.createConversation(conversationPayload, principal.name, principal)

    @PutMapping("/update-name/{conversationId}")
    fun updateConversationName(
        @PathVariable conversationId: String,
        @RequestBody conversationNamePayload: ConversationNamePayload,
        principal: Principal
    ) = conversationService.updateConversationName(conversationId, conversationNamePayload.name, principal)

    @PostMapping("/create/1on1-conversation")
    fun create1on1Conversation(
        @RequestParam userId: String,
        principal: Principal
    ) = conversationService.create1on1Conversation(userId, principal.name, principal)

    @PostMapping("/create/group-conversation")
    fun createGroupConversation(
        @RequestParam groupId: String,
        principal: Principal
    ) = conversationService.createGroupConversation(groupId, principal.name, principal)

    @PutMapping("/{conversationId}/read")
    fun readConversation(
        @PathVariable conversationId: String,
        principal: Principal
    ) = messageService.markAsReadMessage(conversationId, principal.name, principal)

    @PutMapping("/{conversationId}/pin-message/{messageId}")
    fun pinMessage(
        @PathVariable conversationId: String,
        @PathVariable messageId: String,
        principal: Principal
    ) = conversationService.pinConversationMessage(conversationId, messageId, principal)

    @PutMapping("/{conversationId}/unpin-message/{messageId}")
    fun unpinMessage(
        @PathVariable conversationId: String,
        @PathVariable messageId: String,
        principal: Principal
    ) = conversationService.unpinConversationMessage(conversationId, messageId, principal)

    @GetMapping("/{conversationId}/members")
    fun getConversationMembers(
        @PathVariable conversationId: String,
        principal: Principal
    ) = userService.getConversationMembers(conversationId, principal)

    @GetMapping("/{conversationId}/attachments")
    fun getConversationAttachments(
        @PathVariable conversationId: String,
        principal: Principal
    ) = conversationService.getConversationAttachments(conversationId, principal)

    @GetMapping("/check-1on1/{userId}")
    fun check1on1Conversation(
        @PathVariable userId: String,
        principal: Principal
    ): Map<String, Any?> {
        val conversationId = conversationService.check1on1ConversationExists(userId, principal.name, principal)
        return mapOf(
            "exists" to (conversationId != null),
            "conversationId" to conversationId
        )
    }

    @PostMapping("/find-or-create-1on1/{userId}")
    fun findOrCreate1on1Conversation(
        @PathVariable userId: String,
        principal: Principal
    ) = conversationService.findOrCreate1on1Conversation(userId, principal.name, principal)
}