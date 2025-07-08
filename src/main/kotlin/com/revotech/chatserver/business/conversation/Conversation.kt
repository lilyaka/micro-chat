package com.revotech.chatserver.business.conversation

import com.revotech.chatserver.business.message.Message
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

const val DB_GROUP = "chat_conversation"

@Document(DB_GROUP)
class Conversation(
    @Id
    var id: String?,
    var name: String,
    val creatorId: String,
    val adminIds: MutableList<String>,
    var avatar: String?,
    var lastMessage: Message?,
    var isGroup: Boolean,
    var members: MutableList<String>,
    var pin: Message? = null,
    val createdAt: LocalDateTime
) {
    var unread: Int? = 0
    var totalAttachment: Int? = 0

    init {
        // ✅ ENSURE: Group conversation phải có ít nhất 1 admin
        if (isGroup && adminIds.isEmpty()) {
            throw IllegalArgumentException("Group conversation must have at least one admin")
        }

        // ✅ ENSURE: Creator luôn là admin trong group conversation
        if (isGroup && !adminIds.contains(creatorId)) {
            adminIds.add(creatorId)
        }

        // ✅ ENSURE: Creator luôn là member
        if (!members.contains(creatorId)) {
            members.add(creatorId)
        }
    }

    constructor() : this(
        null, "", "", mutableListOf(), "", null, false, mutableListOf(), null, LocalDateTime.now()
    )

    constructor(
        id: String?,
        name: String,
        avatar: String,
        isGroup: Boolean,
        userId: String,
        adminIds: MutableList<String>,
        members: MutableList<String>
    ) : this(
        id,
        name,
        userId,
        // ✅ ENSURE: Creator luôn là admin nếu là group
        ensureCreatorIsAdmin(adminIds, userId, isGroup),
        avatar,
        null,
        isGroup,
        // ✅ ENSURE: Creator luôn là member
        ensureCreatorIsMember(members, userId),
        null,
        LocalDateTime.now()
    )

    companion object {
        /**
         * Đảm bảo creator luôn là admin trong group conversation
         */
        private fun ensureCreatorIsAdmin(
            adminIds: MutableList<String>,
            creatorId: String,
            isGroup: Boolean
        ): MutableList<String> {
            return if (isGroup) {
                adminIds.apply {
                    if (!contains(creatorId)) {
                        add(0, creatorId) // Thêm creator làm admin đầu tiên
                    }
                }
            } else {
                adminIds // 1-on-1 conversation không cần admin
            }
        }

        /**
         * Đảm bảo creator luôn là member
         */
        private fun ensureCreatorIsMember(
            members: MutableList<String>,
            creatorId: String
        ): MutableList<String> {
            return members.apply {
                if (!contains(creatorId)) {
                    add(0, creatorId) // Thêm creator làm member đầu tiên
                }
            }
        }
    }

    /**
     * Kiểm tra user có phải admin không
     */
    fun isAdmin(userId: String): Boolean {
        return isGroup && adminIds.contains(userId)
    }

    /**
     * Kiểm tra user có phải member không
     */
    fun isMember(userId: String): Boolean {
        return members.contains(userId)
    }

    /**
     * Kiểm tra user có phải creator không
     */
    fun isCreator(userId: String): Boolean {
        return creatorId == userId
    }

    /**
     * Validate conversation state
     */
    fun validate() {
        if (isGroup) {
            require(adminIds.isNotEmpty()) {
                "Group conversation must have at least one admin"
            }
            require(adminIds.contains(creatorId)) {
                "Creator must be admin in group conversation"
            }
            require(members.contains(creatorId)) {
                "Creator must be member in conversation"
            }
            require(name.isNotBlank()) {
                "Group conversation must have a name"
            }
        }

        require(members.isNotEmpty()) {
            "Conversation must have at least one member"
        }
    }
}