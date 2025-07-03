package com.revotech.chatserver.controller

import com.revotech.chatserver.business.group.GroupService
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/search")
class SearchController(
    private val userService: UserService,
    private val groupService: GroupService,
    private val messageService: MessageService,
    private val tenantHelper: TenantHelper
) {
    @GetMapping("/user")
    fun searchUser(
        keyword: String,
        principal: Principal
    ) = userService.searchUser(keyword, principal)

    @GetMapping("/group-user-in")
    fun searchGroupUserIn(
        keyword: String,
        principal: Principal
    ) = groupService.searchGroupUserIn(keyword, principal.name, principal)

    @GetMapping("/message")
    fun searchMessage(
        keyword: String,
        principal: Principal
    ) = messageService.searchMessage(keyword, principal)
}