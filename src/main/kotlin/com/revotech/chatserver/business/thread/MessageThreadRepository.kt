package com.revotech.chatserver.business.thread

import org.springframework.data.mongodb.repository.MongoRepository

interface MessageThreadRepository : MongoRepository<MessageThread, String> {
    fun findByParentMessageId(parentMessageId: String): MessageThread?
    fun findByConversationId(conversationId: String): List<MessageThread>
}