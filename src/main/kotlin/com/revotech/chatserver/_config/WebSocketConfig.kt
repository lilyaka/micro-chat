package com.revotech.chatserver._config

import com.revotech.chatserver.business.CHANNEL_DESTINATION
import com.revotech.chatserver.business.CHAT_DESTINATION
import com.revotech.chatserver.business.GROUP_DESTINATION
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean


@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    private val max20Mb = 20 * 1024 * 1024
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker(CHAT_DESTINATION, CHANNEL_DESTINATION, GROUP_DESTINATION)
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/chat-server").setAllowedOriginPatterns("*").withSockJS()
        registry.addEndpoint("/chat-server").setAllowedOriginPatterns("*")
    }

    override fun configureWebSocketTransport(registration: WebSocketTransportRegistration) {
        registration.setMessageSizeLimit(max20Mb)
        registration.setSendBufferSizeLimit(max20Mb)
    }

    @Bean
    fun createServletServerContainerFactoryBean(): ServletServerContainerFactoryBean {
        val container = ServletServerContainerFactoryBean()
        container.setMaxTextMessageBufferSize(max20Mb)
        container.setMaxBinaryMessageBufferSize(max20Mb)
        return container
    }
}
