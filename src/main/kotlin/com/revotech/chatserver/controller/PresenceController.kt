package com.revotech.chatserver.controller

import com.revotech.chatserver.business.presence.UserPresenceService
import com.revotech.chatserver.helper.TenantHelper
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/presence")
class PresenceController(
    private val userPresenceService: UserPresenceService,
    private val tenantHelper: TenantHelper // ✅ Added TenantHelper
) {

    @GetMapping("/online")
    fun getOnlineUsers(principal: Principal): List<String> =
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            userPresenceService.getOnlineUsers()
        }

    @PostMapping("/status")
    fun getUsersStatus(
        @RequestBody userIds: List<String>,
        principal: Principal // ✅ Added principal
    ) = tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
        userPresenceService.getUsersPresence(userIds)
    }

    @GetMapping("/me")
    fun getMyPresence(principal: Principal) =
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            userPresenceService.getUserPresence(principal.name)
        }

    @PostMapping("/activity")
    fun updateActivity(principal: Principal) =
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            userPresenceService.updateUserActivity(principal.name)
        }
}