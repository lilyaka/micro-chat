package com.revotech.chatserver.controller

import com.revotech.chatserver.business.conversation.ConversationMemberService
import com.revotech.chatserver.business.conversation.ConversationService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.payload.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/conversation")
class ConversationController(
    private val conversationService: ConversationService,
    private val messageService: MessageService,
    private val conversationMemberService: ConversationMemberService
) {

    @GetMapping("")
    fun getConversations() = conversationService.getUserConversations()

    /**
     * Smart conversation creation:
     * - 2 users: Creates/finds 1-on-1 conversation
     * - >2 users: Creates group (name required)
     */
    @PostMapping("/create")
    fun createConversation(@ModelAttribute conversationPayload: ConversationPayload) =
        conversationService.createConversation(conversationPayload)

    /**
     * Quick conversation creation with just user IDs
     * Frontend chỉ cần gọi API này với danh sách user IDs
     */
    @PostMapping("/quick-create")
    fun quickCreateConversation(@RequestBody payload: QuickChatPayload) =
        conversationService.createConversation(
            ConversationPayload(
                name = payload.groupName ?: "",
                members = payload.userIds.toMutableList()
            )
        )

    /**
     * Create multi-user chat (always group)
     */
    @PostMapping("/create-group")
    fun createMultiUserChat(@RequestBody payload: MultiUserChatPayload) =
        conversationService.createConversation(
            ConversationPayload(
                name = payload.chatName,
                members = payload.userIds.toMutableList()
            )
        )

    @PutMapping("/update-name/{conversationId}")
    fun updateConversationName(
        @PathVariable conversationId: String,
        @RequestBody conversationNamePayload: ConversationNamePayload
    ) =
        conversationService.updateConversationName(conversationId, conversationNamePayload.name)

    /**
     * Explicit 1-on-1 conversation creation
     */
    @PostMapping("/create/1on1-conversation")
    fun create1on1Conversation(@RequestParam userId: String) =
        conversationService.create1on1Conversation(userId)

    /**
     * Create conversation from existing group
     */
    @PostMapping("/create/group-conversation")
    fun createGroupConversation(@RequestParam groupId: String) =
        conversationService.createGroupConversation(groupId)

    @PutMapping("/{conversationId}/read")
    fun readConversation(@PathVariable conversationId: String) =
        messageService.markAsReadMessage(conversationId)

    @PutMapping("/{conversationId}/pin-message/{messageId}")
    fun pinMessage(@PathVariable conversationId: String, @PathVariable messageId: String) =
        conversationService.pinConversationMessage(conversationId, messageId)

    @PutMapping("/{conversationId}/unpin-message/{messageId}")
    fun unpinMessage(@PathVariable conversationId: String, @PathVariable messageId: String) =
        conversationService.unpinConversationMessage(conversationId, messageId)

    @GetMapping("/{conversationId}/members")
    fun getConversationMembers(@PathVariable conversationId: String) =
        conversationMemberService.getConversationMembersWithPermissions(conversationId)

    @PutMapping("/{conversationId}/members/add")
    fun addConversationMember(
        @PathVariable conversationId: String,
        @RequestParam memberIds: String
    ) = conversationService.addConversationMember(
        conversationId,
        memberIds.split(",").toMutableList()
    )

    @PutMapping("/{conversationId}/members/remove/{memberId}")
    fun removeConversationMember(
        @PathVariable conversationId: String,
        @PathVariable memberId: String
    ) = conversationService.removeConversationMember(conversationId, memberId)

    @DeleteMapping("/{conversationId}")
    fun deleteConversation(@PathVariable conversationId: String) =
        conversationService.deleteConversation(conversationId)

    @GetMapping("/{conversationId}/attachments")
    fun getConversationAttachments(@PathVariable conversationId: String) =
        conversationService.getConversationAttachments(conversationId)

    @GetMapping("/check-1on1/{userId}")
    fun check1on1Conversation(@PathVariable userId: String): Map<String, Any?> {
        val conversationId = conversationService.check1on1ConversationExists(userId)
        return mapOf(
            "exists" to (conversationId != null),
            "conversationId" to conversationId
        )
    }

    @PostMapping("/find-or-create-1on1/{userId}")
    fun findOrCreate1on1Conversation(@PathVariable userId: String) =
        conversationService.findOrCreate1on1Conversation(userId)
}