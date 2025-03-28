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
        adminIds,
        avatar,
        null,
        isGroup,
        members,
        null,
        LocalDateTime.now()
    )
}