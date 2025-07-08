package com.revotech.chatserver.business.topic

import org.springframework.stereotype.Service

@Service
class TopicService(
    private val topicRepository: TopicRepository
) {
    fun getTopic(topicId: String): Topic {
        return topicRepository.findById(topicId).orElseThrow { TopicException("topicNotFound", "Topic not found.") }
    }
}