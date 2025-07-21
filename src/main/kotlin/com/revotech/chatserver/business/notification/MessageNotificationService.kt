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
        println("👂 === MESSAGE NOTIFICATION EVENT RECEIVED ===")

        val payload = event.source as MessageNotificationPayload
        println("👂 Event payload: From=${payload.fromUserId}, To=${payload.toUserId}")

        try {
            sendNotificationToUser(payload)
        } catch (e: Exception) {
            println("❌ Failed to send notification: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun sendNotificationToUser(payload: MessageNotificationPayload) {
        println("🔔 === NOTIFICATION SERVICE DEBUG START ===")
        println("🔔 Payload received:")
        println("   - From User: ${payload.fromUserId}")
        println("   - To User: ${payload.toUserId}")
        println("   - Sender Name: ${payload.senderName}")
        println("   - Content: ${payload.content}")
        println("   - Is Group: ${payload.isGroupMessage}")
        println("   - Conversation: ${payload.conversationName}")

        // ✅ DOUBLE CHECK: Skip nếu same user
        if (payload.fromUserId == payload.toUserId) {
            println("❌ SKIPPING: Same user detected! From=${payload.fromUserId}, To=${payload.toUserId}")
            return
        }

        val title = if (payload.isGroupMessage) {
            payload.conversationName
        } else {
            payload.senderName
        }
        println("🔔 Notification Title: $title")

        val content = buildString {
            if (payload.isGroupMessage) {
                append("${payload.senderName}: ")
            }
            append(payload.content)
        }
        println("🔔 Notification Content: $content")

        try {
            val notification = NotificationEvent(
                NotificationPayload(
                    payload.tenantId,
                    payload.fromUserId,
                    NotificationRequest(
                        userId = payload.toUserId,
                        content = content,
                        module = "CHAT",
                        function = "CHAT/MESSAGE",
                        title = title,
                        action = "OPEN_CONVERSATION"
                    )
                )
            )

            println("📡 Publishing NotificationEvent to library...")
            applicationEventPublisher.publishEvent(notification)
            println("✅ NotificationEvent published successfully!")

        } catch (e: Exception) {
            println("❌ Failed to create/publish NotificationEvent: ${e.message}")
            e.printStackTrace()
        }

        println("🔔 === NOTIFICATION SERVICE DEBUG END ===")
    }
}