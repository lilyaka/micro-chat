package com.revotech.chatserver.controller

import com.revotech.chatserver.business.reaction.MessageReactionService
import com.revotech.chatserver.payload.ReactionPayload
import com.revotech.util.WebUtil
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/reaction")
class ReactionController(
    private val reactionService: MessageReactionService,
    private val webUtil: WebUtil
) {

    @PostMapping("/add")
    fun addReaction(@RequestBody payload: ReactionPayload, principal: Principal) {
        val userId = principal.name
        reactionService.addReaction(payload.messageId, payload.emoji, userId)
    }

    @DeleteMapping("/remove")
    fun removeReaction(@RequestBody payload: ReactionPayload, principal: Principal) {
        val userId = principal.name
        reactionService.removeReaction(payload.messageId, payload.emoji, userId)
    }

    @GetMapping("/message/{messageId}")
    fun getReactions(@PathVariable messageId: String) =
        reactionService.getReactionSummary(messageId)
}