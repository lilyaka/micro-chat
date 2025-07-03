package com.revotech.chatserver.controller

import com.revotech.chatserver.business.reaction.MessageReactionService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.ReactionPayload
import com.revotech.util.WebUtil
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/reaction")
class ReactionController(
    private val reactionService: MessageReactionService,
    private val webUtil: WebUtil,
    private val tenantHelper: TenantHelper
) {

    @PostMapping("/add")
    fun addReaction(
        @RequestBody payload: ReactionPayload,
        principal: Principal
    ) {
        val userId = principal.name
        reactionService.addReaction(payload.messageId, payload.emoji, userId, principal)
    }

    @DeleteMapping("/remove")
    fun removeReaction(
        @RequestBody payload: ReactionPayload,
        principal: Principal
    ) {
        val userId = principal.name
        reactionService.removeReaction(payload.messageId, payload.emoji, userId, principal)
    }

    @GetMapping("/message/{messageId}")
    fun getReactions(
        @PathVariable messageId: String,
        principal: Principal
    ) = reactionService.getReactionSummary(messageId, principal)
}