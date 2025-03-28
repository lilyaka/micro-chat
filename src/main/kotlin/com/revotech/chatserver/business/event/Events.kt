package com.revotech.chatserver.business.event

import com.revotech.chatserver.business.conversation.Conversation
import org.springframework.context.ApplicationEvent

//class CreateConversationEvent(createConversationEventPayload: CreateConversationEventPayload) :
//    ApplicationEvent(createConversationEventPayload)

class CreateConversationEvent(conversation: Conversation) : ApplicationEvent(conversation)

class DeleteConversationEvent(conversationId: String) : ApplicationEvent(conversationId)

data class CreateConversationEventPayload(val tenantId: String, val conversation: Conversation)
