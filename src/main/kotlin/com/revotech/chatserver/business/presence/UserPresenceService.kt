package com.revotech.chatserver.business.presence

import com.revotech.chatserver.business.CHANNEL_DESTINATION
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class UserPresenceService {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    // Lazy initialization để tránh circular dependency
    private val simpMessagingTemplate: SimpMessagingTemplate by lazy {
        applicationContext.getBean(SimpMessagingTemplate::class.java)
    }

    // Map<userId, UserPresence>
    private val userPresences = ConcurrentHashMap<String, UserPresence>()

    // Thresholds
    private val AWAY_THRESHOLD_MINUTES = 5L
    private val OFFLINE_THRESHOLD_MINUTES = 15L

    fun addUserSession(userId: String, sessionId: String) {
        val presence = userPresences.computeIfAbsent(userId) {
            UserPresence(userId, PresenceStatus.ONLINE, LocalDateTime.now())
        }

        presence.sessions.add(sessionId)
        presence.status = PresenceStatus.ONLINE
        presence.lastActivity = LocalDateTime.now()
        presence.lastSeen = LocalDateTime.now()

        broadcastPresenceUpdate(presence)
    }

    fun removeUserSession(userId: String, sessionId: String) {
        val presence = userPresences[userId] ?: return

        presence.sessions.remove(sessionId)
        presence.lastSeen = LocalDateTime.now()

        // Nếu không còn session nào -> offline
        if (presence.sessions.isEmpty()) {
            presence.status = PresenceStatus.OFFLINE
            broadcastPresenceUpdate(presence)
        } else {
            // Vẫn còn sessions khác -> vẫn online
            presence.status = PresenceStatus.ONLINE
            broadcastPresenceUpdate(presence)
        }
    }

    fun updateUserActivity(userId: String) {
        val presence = userPresences[userId] ?: return

        presence.lastActivity = LocalDateTime.now()

        // Chỉ update status nếu đang away
        if (presence.status == PresenceStatus.AWAY && presence.sessions.isNotEmpty()) {
            presence.status = PresenceStatus.ONLINE
            broadcastPresenceUpdate(presence)
        }
    }

    fun getUserPresence(userId: String): UserPresence? {
        return userPresences[userId]
    }

    fun getOnlineUsers(): List<String> {
        return userPresences.values
            .filter { it.status == PresenceStatus.ONLINE }
            .map { it.userId }
    }

    fun getUsersPresence(userIds: List<String>): Map<String, PresenceStatus> {
        return userIds.associateWith { userId ->
            userPresences[userId]?.status ?: PresenceStatus.OFFLINE
        }
    }

    private fun broadcastPresenceUpdate(presence: UserPresence) {
        try {
            val update = PresenceUpdate(
                userId = presence.userId,
                status = presence.status,
                lastSeen = presence.lastSeen,
                sessionCount = presence.sessions.size
            )

            // Broadcast to all users who need to know this user's status
            simpMessagingTemplate.convertAndSend(
                "$CHANNEL_DESTINATION/presence",
                update
            )
        } catch (e: Exception) {
            // Log error but don't fail the operation
            println("Failed to broadcast presence update: ${e.message}")
        }
    }

    // Auto check status every minute
    @Scheduled(fixedRate = 60000) // Every 1 minute
    fun checkUserStatuses() {
        val now = LocalDateTime.now()

        userPresences.values.forEach { presence ->
            val minutesSinceActivity = java.time.Duration.between(presence.lastActivity, now).toMinutes()

            val newStatus = when {
                presence.sessions.isEmpty() -> PresenceStatus.OFFLINE
                minutesSinceActivity >= OFFLINE_THRESHOLD_MINUTES -> PresenceStatus.OFFLINE
                minutesSinceActivity >= AWAY_THRESHOLD_MINUTES -> PresenceStatus.AWAY
                else -> PresenceStatus.ONLINE
            }

            if (newStatus != presence.status) {
                presence.status = newStatus
                if (newStatus == PresenceStatus.OFFLINE) {
                    presence.sessions.clear() // Clear sessions when offline
                }
                broadcastPresenceUpdate(presence)
            }
        }

        // Cleanup offline users after long time
        val cutoffTime = now.minusHours(24)
        userPresences.entries.removeIf { (_, presence) ->
            presence.status == PresenceStatus.OFFLINE && presence.lastSeen.isBefore(cutoffTime)
        }
    }
}