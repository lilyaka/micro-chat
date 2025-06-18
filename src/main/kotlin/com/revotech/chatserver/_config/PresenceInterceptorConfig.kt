package com.revotech.chatserver._config

import com.revotech.chatserver.business.presence.UserPresenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
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
class PresenceInterceptorConfig : WebSocketMessageBrokerConfigurer {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(PresenceActivityInterceptor(applicationContext))
    }
}

class PresenceActivityInterceptor(
    private val applicationContext: ApplicationContext
) : ChannelInterceptor {

    // Lazy initialization để tránh circular dependency
    private val userPresenceService: UserPresenceService by lazy {
        applicationContext.getBean(UserPresenceService::class.java)
    }

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        // Track activity for all commands except CONNECT/DISCONNECT
        if (accessor?.command != StompCommand.CONNECT &&
            accessor?.command != StompCommand.DISCONNECT &&
            accessor?.user != null) {

            try {
                userPresenceService.updateUserActivity(accessor.user!!.name)
            } catch (e: Exception) {
                // Log error but don't fail the message processing
                println("Failed to update user activity: ${e.message}")
            }
        }

        return message
    }
}