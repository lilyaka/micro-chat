package com.revotech.chatserver.business.aop

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
        "@annotation(com.revotech.chatserver.business.aop.AfterDeleteConversation)",
        returning = "conversationId"
    )
    fun doAfterDeleteConversation(jp: JoinPoint, conversationId: String) {
        applicationEventPublisher.publishEvent(
            DeleteConversationEvent(conversationId)
        )
    }
}
