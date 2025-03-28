package com.revotech.chatserver.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.messaging.MessagingException
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtUtil(private val secretKey: SecretKey) {
    fun getClaims(token: String): Claims {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token).payload
        } catch (e: Exception) {
            throw MessagingException("invalidJwt")
        }
    }
}
