package com.revotech.chatserver.business.event

import org.springframework.context.ApplicationEvent

class MessageNotificationEvent(
    notificationPayload: MessageNotificationPayload
) : ApplicationEvent(notificationPayload)

data class MessageNotificationPayload(
    val tenantId: String,
    val fromUserId: String,
    val toUserId: String,
    val messageId: String,
    val conversationId: String,
    val conversationName: String,
    val senderName: String,
    val content: String,
    val isGroupMessage: Boolean
)