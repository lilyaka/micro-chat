package com.revotech.chatserver.helper

import com.revotech.chatserver.jwt.JwtUtil
import io.jsonwebtoken.Claims
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.security.Principal

@Component
class TokenHelper(
    private val request: HttpServletRequest,
    private val jwtUtil: JwtUtil,
) {
    fun getTenantId(): String? {
        val token = request.getHeader("Authorization")
        return getTenantId(token.removePrefix("Bearer "))
    }

    fun toPrincipal(accessToken: String): Principal {
        val claims = jwtUtil.getClaims(accessToken)
        return UsernamePasswordAuthenticationToken(
            claims.subject, claims, getAuthoritiesFromClaims(claims)
        )
    }

    private fun getAuthoritiesFromClaims(claims: Claims): List<GrantedAuthority>? {
        return (claims["roles"] as String?)
            ?.split(",")
            ?.map { SimpleGrantedAuthority(it.trim()) }
    }

    fun getTenantId(accessToken: String): String? {
        val claims = jwtUtil.getClaims(accessToken)
        return claims["tenant"] as String?
    }
}
