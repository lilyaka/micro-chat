package com.revotech.chatserver.business.reaction

import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository

interface MessageReactionRepository : MongoRepository<MessageReaction, String> {
    fun findByMessageId(messageId: String): List<MessageReaction>
    fun findByMessageIdAndUserId(messageId: String, userId: String): List<MessageReaction>
    fun deleteByMessageIdAndUserIdAndEmoji(messageId: String, userId: String, emoji: String)

    @Aggregation(pipeline = [
        """{ ${'$'}match: { messageId: ?0 } }""",
        """{ ${'$'}group: { 
        _id: "${'$'}emoji", 
        count: { ${'$'}sum: 1 },
        users: { ${'$'}push: "${'$'}userId" }
    }}""",
        """{ ${'$'}project: {
        emoji: "${'$'}_id",
        count: 1,
        users: 1,
        _id: 0
    }}"""
    ])
    fun getReactionSummary(messageId: String): List<ReactionSummary>
}

data class ReactionSummary(
    val emoji: String,
    val count: Int,
    val users: List<String>
)