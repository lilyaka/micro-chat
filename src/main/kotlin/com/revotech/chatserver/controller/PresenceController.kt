package com.revotech.chatserver.controller

import com.revotech.chatserver.business.presence.UserPresenceService
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/presence")
class PresenceController(
    private val userPresenceService: UserPresenceService
) {

    @GetMapping("/online")
    fun getOnlineUsers(): List<String> {
        return userPresenceService.getOnlineUsers()
    }

    @PostMapping("/status")
    fun getUsersStatus(@RequestBody userIds: List<String>) =
        userPresenceService.getUsersPresence(userIds)

    @GetMapping("/me")
    fun getMyPresence(principal: Principal) =
        userPresenceService.getUserPresence(principal.name)

    @PostMapping("/activity")
    fun updateActivity(principal: Principal) {
        userPresenceService.updateUserActivity(principal.name)
    }
}