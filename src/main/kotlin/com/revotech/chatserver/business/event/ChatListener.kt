package com.revotech.chatserver.business.event

import com.revotech.chatserver.business.conversation.Conversation
import com.revotech.chatserver.business.conversation.ConversationService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ChatListener(
    private val conversationService: ConversationService
) {
    @Async
    @EventListener
    fun deleteConversationEvent(event: DeleteConversationEvent) {
        val source = event.source as String
        conversationService.deleteConversationFolder(source)
    }
}