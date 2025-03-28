package com.revotech.chatserver.business.topicComment

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface TopicCommentRepository : MongoRepository<TopicComment, String> {
    fun findByTopicId(topicId: String, pageable: Pageable) : Page<TopicComment>
}
