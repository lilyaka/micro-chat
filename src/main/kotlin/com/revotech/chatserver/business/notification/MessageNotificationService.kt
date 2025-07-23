package com.revotech.chatserver.business.notification

import com.revotech.chatserver.business.event.MessageNotificationEvent
import com.revotech.chatserver.business.event.MessageNotificationPayload
import com.revotech.dto.NotificationRequest
import com.revotech.event.notification.NotificationEvent
import com.revotech.event.notification.NotificationPayload
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class MessageNotificationService(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    @EventListener
    fun handleMessageNotification(event: MessageNotificationEvent) {
        val payload = event.source as MessageNotificationPayload
        try {
            sendNotificationToUser(payload)
        } catch (e: Exception) {
            println("❌ Failed to send notification: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun sendNotificationToUser(payload: MessageNotificationPayload) {

        if (payload.fromUserId == payload.toUserId) {
            return
        }

        val title = if (payload.isGroupMessage) {
            payload.conversationName
        } else {
            payload.senderName
        }

        val content = buildString {
            if (payload.isGroupMessage) {
                append("${payload.senderName}: ")
            }
            append(payload.content)
        }

        try {
            val notification = NotificationEvent(
                NotificationPayload(
                    payload.tenantId,
                    payload.toUserId,
                    NotificationRequest(
                        userId = payload.toUserId,
                        content = content,
                        module = "CHAT",
                        function = "CHAT/MESSAGE",
                        title = title,
                        action = "OPEN_CONVERSATION",
                        fromUserId = payload.fromUserId
                    )
                )
            )
            applicationEventPublisher.publishEvent(notification)

        } catch (e: Exception) {
            println("❌ Failed to create/publish NotificationEvent: ${e.message}")
            e.printStackTrace()
        }
    }
}