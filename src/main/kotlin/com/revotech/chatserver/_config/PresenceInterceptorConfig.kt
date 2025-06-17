package com.revotech.chatserver._config

import com.revotech.chatserver.business.presence.UserPresenceService
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@Order(1)
class PresenceInterceptorConfig(
    private val userPresenceService: UserPresenceService
) : WebSocketMessageBrokerConfigurer {

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(PresenceActivityInterceptor(userPresenceService))
    }
}

class PresenceActivityInterceptor(
    private val userPresenceService: UserPresenceService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        // Track activity for all commands except CONNECT/DISCONNECT
        if (accessor?.command != StompCommand.CONNECT &&
            accessor?.command != StompCommand.DISCONNECT &&
            accessor?.user != null) {

            userPresenceService.updateUserActivity(accessor.user!!.name)
        }

        return message
    }
}
