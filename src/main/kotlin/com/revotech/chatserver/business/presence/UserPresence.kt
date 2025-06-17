package com.revotech.chatserver.business.presence

import java.time.LocalDateTime

data class UserPresence(
    val userId: String,
    var status: PresenceStatus,
    var lastSeen: LocalDateTime,
    val sessions: MutableSet<String> = mutableSetOf(), // Track multiple tabs/devices
    var lastActivity: LocalDateTime = LocalDateTime.now()
)

enum class PresenceStatus {
    ONLINE,
    AWAY,     // Không active trong 5 phút
    OFFLINE
}

data class PresenceUpdate(
    val userId: String,
    val status: PresenceStatus,
    val lastSeen: LocalDateTime,
    val sessionCount: Int // Number of active sessions
)