package com.revotech.chatserver.business.presence

import com.revotech.chatserver.helper.TokenHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketPresenceListener(
    private val tokenHelper: TokenHelper
) {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    // Lazy initialization để tránh circular dependency
    private val userPresenceService: UserPresenceService by lazy {
        applicationContext.getBean(UserPresenceService::class.java)
    }
    
    private val typingService by lazy {
        try {
            applicationContext.getBean("typingService", com.revotech.chatserver.business.typing.TypingService::class.java)
        } catch (e: Exception) {
            println("TypingService not found: ${e.message}")
            null
        }
    }

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val principal = headerAccessor.user

        if (principal != null) {
            val userId = principal.name
            val sessionId = headerAccessor.sessionId ?: return

            try {
                userPresenceService.addUserSession(userId, sessionId)
            } catch (e: Exception) {
                println("❌ WebSocketPresenceListener: Failed to add user session: ${e.message}")
            }
        }
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val principal = headerAccessor.user

        if (principal != null) {
            val userId = principal.name
            val sessionId = headerAccessor.sessionId ?: return

            try {
                // ✅ Cleanup presence
                userPresenceService.removeUserSession(userId, sessionId)

                typingService?.cleanupUserTyping(userId)

            } catch (e: Exception) {
                println("❌ WebSocketPresenceListener: Failed to cleanup user session: ${e.message}")
            }
        }
    }
}