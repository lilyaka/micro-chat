package com.revotech.chatserver.business.topic

import org.bson.codecs.pojo.annotations.BsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("topic")
data class Topic(
    @Id
    var id: String?,
    val creatorId: String?,
    val createdAt: LocalDateTime?,
    var lastSentAt: LocalDateTime?,
    val name: String,
    val subjectId: String,
    val type: TopicType,
    val isDeleted: Boolean = false,
    @BsonIgnore
    val creator: String? = ""
)

enum class TopicType {
    ORGANIZATION,
    GROUP
}