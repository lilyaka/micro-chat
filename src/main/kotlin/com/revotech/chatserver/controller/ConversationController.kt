package com.revotech.chatserver.controller

import com.revotech.chatserver.business.conversation.ConversationService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.payload.*
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/conversation")
class ConversationController(
    private val conversationService: ConversationService,
    private val messageService: MessageService,
    private val userService: UserService
) {

    @GetMapping("")
    fun getConversations(principal: Principal) = conversationService.getUserConversations(principal.name)

    /**
     * Smart conversation creation:
     * - 2 users: Creates/finds 1-on-1 conversation
     * - >2 users: Creates group (name required)
     */
    @PostMapping("/create")
    fun createConversation(
        @ModelAttribute conversationPayload: ConversationPayload,
        principal: Principal
    ) = conversationService.createConversation(conversationPayload, principal.name)

    /**
     * Quick conversation creation with just user IDs
     * Frontend chỉ cần gọi API này với danh sách user IDs
     */
    @PostMapping("/quick-create")
    fun quickCreateConversation(
        @RequestBody payload: QuickChatPayload,
        principal: Principal
    ) = conversationService.createConversation(
        ConversationPayload(
            name = payload.groupName ?: "",
            members = payload.userIds.toMutableList()
        ),
        principal.name
    )

    /**
     * Create multi-user chat (always group)
     */
    @PostMapping("/create-group")
    fun createMultiUserChat(
        @RequestBody payload: MultiUserChatPayload,
        principal: Principal
    ) = conversationService.createConversation(
        ConversationPayload(
            name = payload.chatName,
            members = payload.userIds.toMutableList()
        ),
        principal.name
    )

    @PutMapping("/update-name/{conversationId}")
    fun updateConversationName(
        @PathVariable conversationId: String,
        @RequestBody conversationNamePayload: ConversationNamePayload
    ) = conversationService.updateConversationName(conversationId, conversationNamePayload.name)

    /**
     * Explicit 1-on-1 conversation creation
     */
    @PostMapping("/create/1on1-conversation")
    fun create1on1Conversation(
        @RequestParam userId: String,
        principal: Principal
    ) = conversationService.create1on1Conversation(userId, principal.name)

    /**
     * Create conversation from existing group
     */
    @PostMapping("/create/group-conversation")
    fun createGroupConversation(
        @RequestParam groupId: String,
        principal: Principal
    ) = conversationService.createGroupConversation(groupId, principal.name)

    @PutMapping("/{conversationId}/read")
    fun readConversation(
        @PathVariable conversationId: String,
        principal: Principal
    ) = messageService.markAsReadMessage(conversationId, principal.name)

    @PutMapping("/{conversationId}/pin-message/{messageId}")
    fun pinMessage(@PathVariable conversationId: String, @PathVariable messageId: String) =
        conversationService.pinConversationMessage(conversationId, messageId)

    @PutMapping("/{conversationId}/unpin-message/{messageId}")
    fun unpinMessage(@PathVariable conversationId: String, @PathVariable messageId: String) =
        conversationService.unpinConversationMessage(conversationId, messageId)

    @GetMapping("/{conversationId}/members")
    fun getConversationMembers(@PathVariable conversationId: String) =
        userService.getConversationMembers(conversationId)

    @PutMapping("/{conversationId}/members/add")
    fun addConversationMember(
        @PathVariable conversationId: String,
        @RequestParam memberIds: String,
        principal: Principal
    ) = conversationService.addConversationMember(
        conversationId,
        memberIds.split(",").toMutableList(),
        principal.name
    )

    @PutMapping("/{conversationId}/members/remove/{memberId}")
    fun removeConversationMember(
        @PathVariable conversationId: String,
        @PathVariable memberId: String,
        principal: Principal
    ) = conversationService.removeConversationMember(conversationId, memberId, principal.name)

    @DeleteMapping("/{conversationId}")
    fun deleteConversation(
        @PathVariable conversationId: String,
        principal: Principal
    ) = conversationService.deleteConversation(conversationId, principal.name)

    @GetMapping("/{conversationId}/attachments")
    fun getConversationAttachments(@PathVariable conversationId: String) =
        conversationService.getConversationAttachments(conversationId)

    @GetMapping("/check-1on1/{userId}")
    fun check1on1Conversation(
        @PathVariable userId: String,
        principal: Principal
    ): Map<String, Any?> {
        val conversationId = conversationService.check1on1ConversationExists(userId, principal.name)
        return mapOf(
            "exists" to (conversationId != null),
            "conversationId" to conversationId
        )
    }

    @PostMapping("/find-or-create-1on1/{userId}")
    fun findOrCreate1on1Conversation(
        @PathVariable userId: String,
        principal: Principal
    ) = conversationService.findOrCreate1on1Conversation(userId, principal.name)
}