package com.revotech.chatserver.business.presence

import com.revotech.chatserver.helper.TokenHelper
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketPresenceListener(
    private val userPresenceService: UserPresenceService,
    private val tokenHelper: TokenHelper
) {

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val principal = headerAccessor.user

        if (principal != null) {
            val userId = principal.name
            val sessionId = headerAccessor.sessionId ?: return

            userPresenceService.addUserSession(userId, sessionId)
        }
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val principal = headerAccessor.user

        if (principal != null) {
            val userId = principal.name
            val sessionId = headerAccessor.sessionId ?: return

            userPresenceService.removeUserSession(userId, sessionId)
        }
    }
}