package com.revotech.chatserver.business.aop


import com.revotech.chatserver.business.conversation.Conversation
import com.revotech.chatserver.business.event.CreateConversationEvent
import com.revotech.chatserver.business.event.DeleteConversationEvent
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
@Aspect
class ChatAspect(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @AfterReturning(
        "@annotation(com.revotech.chatserver.business.aop.AfterCreateConversation)",
        returning = "conversation"
    )
    fun doAfterCreateConversation(jp: JoinPoint, conversation: Conversation) {
        applicationEventPublisher.publishEvent(
            CreateConversationEvent(conversation)
        )
    }

    @AfterReturning(
        "@annotation(com.revotech.chatserver.business.aop.AfterDeleteConversation)",
        returning = "conversationId"
    )
    fun doAfterDeleteConversation(jp: JoinPoint, conversationId: String) {
        applicationEventPublisher.publishEvent(
            DeleteConversationEvent(conversationId)
        )
    }
}
