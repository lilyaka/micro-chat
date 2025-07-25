package com.revotech.chatserver.controller

import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.helper.TokenHelper
import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class WebSocketController(
    private val simpUserRegistry: SimpUserRegistry,
    private val tenantHelper: TenantHelper,
    private val tokenHelper: TokenHelper,
) {
    @GetMapping("/ws/users")
    fun currentUsers(): List<String> {
        return simpUserRegistry
            .users
            .filter {
                tokenHelper.getTenantId() == tenantHelper.getTenantId(it.principal as AbstractAuthenticationToken)
            }
            .map { it.name }
            .distinct()
    }
}
