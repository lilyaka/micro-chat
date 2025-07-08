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
        messageStatusService.markAsDelivered(payload.messageId, principal.name, principal)
    }

    @MessageMapping("/chat/read")
    fun handleMessageRead(@Payload payload: MessageStatusPayload, principal: Principal) {
        messageStatusService.markAsRead(payload.messageId, principal.name, principal)
    }

    @MessageMapping("/chat/conversation/read")
    fun handleConversationRead(@Payload payload: ConversationReadPayload, principal: Principal) {
        messageStatusService.markConversationAsRead(payload.conversationId, principal.name, principal)
    }

    @MessageMapping("/chat/user/online")
    fun handleUserOnline(principal: Principal) {
        messageStatusService.handleUserOnline(principal.name, principal)
    }
}

data class ConversationReadPayload(
    val conversationId: String
)