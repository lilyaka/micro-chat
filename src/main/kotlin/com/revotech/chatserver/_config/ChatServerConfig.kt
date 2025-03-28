package com.revotech.chatserver._config

import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.crypto.SecretKey
import kotlin.random.Random

@Configuration
class ChatServerConfig {
    @Value("\${auth.key.seed}")
    private var seed: Int = 0

    @Bean
    fun generateKey(): SecretKey {
        val random = Random(seed)
        val bytes = random.nextBytes(64)
        return Keys.hmacShaKeyFor(bytes)
    }
}
