package com.revotech.chatserver.controller

import com.revotech.chatserver.business.conversation.ConversationService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.payload.ConversationNamePayload
import com.revotech.chatserver.payload.ConversationPayload
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/conversation")
class ConversationController(
    private val conversationService: ConversationService,
    private val messageService: MessageService,
    private val userService: UserService
) {

    @GetMapping("")
    fun getConversations() = conversationService.getUserConversations()

    @PostMapping("/create")
    fun createConversation(@ModelAttribute conversationPayload: ConversationPayload) =
        conversationService.createConversation(conversationPayload)

    @PutMapping("/update-name/{conversationId}")
    fun updateConversationName(
        @PathVariable conversationId: String,
        @RequestBody conversationNamePayload: ConversationNamePayload
    ) =
        conversationService.updateConversationName(conversationId, conversationNamePayload.name)

    @PostMapping("/create/1on1-conversation")
    fun create1on1Conversation(userId: String) = conversationService.create1on1Conversation(userId)

    @PostMapping("/create/group-conversation")
    fun createGroupConversation(groupId: String) = conversationService.createGroupConversation(groupId)

    @PutMapping("/{conversationId}/read")
    fun readConversation(@PathVariable conversationId: String) = messageService.markAsReadMessage(conversationId)

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
    fun addConversationMember(@PathVariable conversationId: String, memberIds: String) =
        conversationService.addConversationMember(conversationId, memberIds.split(",").toMutableList())

    @PutMapping("/{conversationId}/members/remove/{memberId}")
    fun removeConversationMember(@PathVariable conversationId: String, @PathVariable memberId: String) =
        conversationService.removeConversationMember(conversationId, memberId)

    @DeleteMapping("/{conversationId}")
    fun deleteConversation(@PathVariable conversationId: String) =
        conversationService.deleteConversation(conversationId)

    @GetMapping("/{conversationId}/attachments")
    fun getConversationAttachments(@PathVariable conversationId: String) =
        conversationService.getConversationAttachments(conversationId)
}
