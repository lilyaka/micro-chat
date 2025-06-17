package com.revotech.chatserver.controller

import com.revotech.chatserver.business.message.status.MessageStatusService
import com.revotech.chatserver.payload.MessageStatusPayload
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class StatusWebSocketHandler(
    private val messageStatusService: MessageStatusService,
    private val simpMessagingTemplate: SimpMessagingTemplate
) {

    @MessageMapping("/chat/delivered")
    fun handleMessageDelivered(@Payload payload: MessageStatusPayload, principal: Principal) {
        val userId = principal.name
        messageStatusService.markAsDelivered(payload.messageId, userId)
    }

    @MessageMapping("/chat/read")
    fun handleMessageRead(@Payload payload: MessageStatusPayload, principal: Principal) {
        val userId = principal.name
        messageStatusService.markAsRead(payload.messageId, userId)
    }

    @MessageMapping("/chat/conversation/read")
    fun handleConversationRead(@Payload payload: ConversationReadPayload, principal: Principal) {
        val userId = principal.name
        messageStatusService.markConversationAsRead(payload.conversationId, userId)
    }

    @MessageMapping("/chat/user/online")
    fun handleUserOnline(principal: Principal) {
        val userId = principal.name
        messageStatusService.handleUserOnline(userId)
    }
}

data class ConversationReadPayload(
    val conversationId: String
)