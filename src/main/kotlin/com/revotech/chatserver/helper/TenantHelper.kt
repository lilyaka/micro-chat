package com.revotech.chatserver.helper

import com.revotech.config.multitenant.TenantContext.currentTenant
import io.jsonwebtoken.Claims
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Component

@Component
class TenantHelper {
    fun <T> changeTenant(tenant: String, action: () -> T): T {
        try {
            currentTenant = tenant
            return action()
        } finally {
            currentTenant = null
        }
    }

    fun <T> changeTenant(token: AbstractAuthenticationToken, action: () -> T): T {
        return changeTenant(getTenantId(token), action)
    }
    fun getTenantId(token: AbstractAuthenticationToken): String {
        return try {
            val claims = token.credentials as Claims
            claims["tenant"] as String
        } catch (_: Exception) {
            ""
        }
    }
}
