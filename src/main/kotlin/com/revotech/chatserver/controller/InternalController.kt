package com.revotech.chatserver.controller

import com.revotech.chatserver.business.conversation.ConversationService
import com.revotech.chatserver.business.group.GroupService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.business.reaction.MessageReactionService
import com.revotech.chatserver.business.thread.MessageThreadService
import com.revotech.chatserver.business.presence.UserPresenceService
import com.revotech.chatserver.payload.*
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/**
 * REST API Controller - Để phục vụ ChatServerClient từ main service
 * Endpoints này PHẢI khớp với những gì ChatServerClient đang gọi
 */
@RestController
@RequestMapping("") // Internal prefix để phân biệt
class InternalController(
    private val conversationService: ConversationService,
    private val messageService: MessageService,
    private val userService: UserService,
    private val reactionService: MessageReactionService,
    private val threadService: MessageThreadService,
    private val userPresenceService: UserPresenceService,
    private val groupService: GroupService
) {

    // === CONVERSATION APIs ===
    @GetMapping("/conversations")
    fun getConversations() = conversationService.getUserConversations()

    @PostMapping("/conversation/create")
    fun createConversation(@RequestBody conversationPayload: ConversationPayload) =
        conversationService.createConversation(conversationPayload)

    @PostMapping("/conversation/quick-create")
    fun quickCreateConversation(@RequestBody payload: QuickChatPayload) =
        conversationService.createConversation(
            ConversationPayload(
                name = payload.groupName ?: "",
                members = payload.userIds.toMutableList()
            )
        )

    @PutMapping("/conversation/{conversationId}/name")
    fun updateConversationName(
        @PathVariable conversationId: String,
        @RequestBody payload: ConversationNamePayload
    ) = conversationService.updateConversationName(conversationId, payload.name)

    @PostMapping("/conversation/1on1/{userId}")
    fun create1on1Conversation(@PathVariable userId: String) =
        conversationService.create1on1Conversation(userId)

    @PostMapping("/conversation/from-group/{groupId}")
    fun createGroupConversation(@PathVariable groupId: String) =
        conversationService.createGroupConversation(groupId)

    @PutMapping("/conversation/{conversationId}/read")
    fun markConversationAsRead(@PathVariable conversationId: String) =
        messageService.markAsReadMessage(conversationId)

    @PutMapping("/conversation/{conversationId}/pin/{messageId}")
    fun pinMessage(@PathVariable conversationId: String, @PathVariable messageId: String) =
        conversationService.pinConversationMessage(conversationId, messageId)

    @PutMapping("/conversation/{conversationId}/unpin/{messageId}")
    fun unpinMessage(@PathVariable conversationId: String, @PathVariable messageId: String) =
        conversationService.unpinConversationMessage(conversationId, messageId)

    @GetMapping("/conversation/{conversationId}/members")
    fun getConversationMembers(@PathVariable conversationId: String) =
        userService.getConversationMembers(conversationId)

    @PutMapping("/conversation/{conversationId}/members")
    fun addConversationMembers(
        @PathVariable conversationId: String,
        @RequestBody memberIds: List<String>
    ) = conversationService.addConversationMember(conversationId, memberIds.toMutableList())

    @DeleteMapping("/conversation/{conversationId}/members/{memberId}")
    fun removeConversationMember(
        @PathVariable conversationId: String,
        @PathVariable memberId: String
    ) = conversationService.removeConversationMember(conversationId, memberId)

    @DeleteMapping("/conversation/{conversationId}")
    fun deleteConversation(@PathVariable conversationId: String) =
        conversationService.deleteConversation(conversationId)

    @GetMapping("/conversation/{conversationId}/attachments")
    fun getConversationAttachments(@PathVariable conversationId: String) =
        conversationService.getConversationAttachments(conversationId)

    @GetMapping("/conversation/check-1on1/{userId}")
    fun check1on1Conversation(@PathVariable userId: String): Map<String, Any?> {
        val conversationId = conversationService.check1on1ConversationExists(userId)
        return mapOf(
            "exists" to (conversationId != null),
            "conversationId" to conversationId
        )
    }

    // === MESSAGE APIs ===
    @GetMapping("/conversation/{conversationId}/messages")
    fun getConversationHistory(
        @PathVariable conversationId: String,
        pageable: Pageable
    ) = messageService.getHistories(conversationId, pageable)

    @GetMapping("/message/{messageId}/context")
    fun getMessageContext(
        @PathVariable messageId: String,
        @RequestParam conversationId: String,
        @RequestParam(defaultValue = "20") pageSize: Int
    ) = messageService.getMessageContext(conversationId, messageId, pageSize)

    @GetMapping("/attachment/{attachmentId}")
    fun getAttachment(@PathVariable attachmentId: String) =
        messageService.getAttachment(attachmentId)

    // === THREAD APIs ===
    @PostMapping("/thread/create/{parentMessageId}")
    fun createThread(@PathVariable parentMessageId: String) =
        threadService.createThread(parentMessageId, "system") // Need to get userId from headers

    @GetMapping("/thread/{threadId}/replies")
    fun getThreadReplies(@PathVariable threadId: String, pageable: Pageable) =
        threadService.getThreadReplies(threadId, pageable)

    @GetMapping("/thread/summary/{parentMessageId}")
    fun getThreadSummary(@PathVariable parentMessageId: String) =
        threadService.getThreadSummary(parentMessageId)

    // === REACTION APIs ===
    @GetMapping("/message/{messageId}/reactions")
    fun getMessageReactions(@PathVariable messageId: String) =
        reactionService.getReactionSummary(messageId)

    // === PRESENCE APIs ===
    @GetMapping("/presence/online")
    fun getOnlineUsers() = userPresenceService.getOnlineUsers()

    @PostMapping("/presence/status")
    fun getUsersStatus(@RequestBody userIds: List<String>) =
        userPresenceService.getUsersPresence(userIds)

    // === SEARCH APIs ===
    @GetMapping("/search/users")
    fun searchUsers(@RequestParam keyword: String) = userService.searchUser(keyword)

    @GetMapping("/search/groups")
    fun searchGroups(@RequestParam keyword: String) = groupService.searchGroupUserIn(keyword)

    @GetMapping("/search/messages")
    fun searchMessages(@RequestParam keyword: String) = messageService.searchMessage(keyword)
}