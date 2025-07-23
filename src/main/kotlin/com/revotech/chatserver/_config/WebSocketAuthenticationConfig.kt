package com.revotech.chatserver._config

import com.revotech.chatserver.helper.TokenHelper
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
@Order(0)
class WebSocketAuthenticationConfig(
    private val tokenHelper: TokenHelper,
) : WebSocketMessageBrokerConfigurer {
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(CustomChannelInterceptor(tokenHelper))
    }
}

class CustomChannelInterceptor(
    private val tokenHelper: TokenHelper,
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (StompCommand.CONNECT == accessor!!.command) {
            val authorization = accessor.getNativeHeader("Authorization")
            val accessToken = authorization!![0].split(" ").dropLastWhile { it.isEmpty() }.last()
            accessor.user = tokenHelper.toPrincipal(accessToken)
        }

        return message
    }
}
