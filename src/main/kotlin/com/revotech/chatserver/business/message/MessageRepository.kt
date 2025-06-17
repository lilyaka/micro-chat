package com.revotech.chatserver.business.message

import com.revotech.chatserver.business.attachment.Attachment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.time.LocalDateTime
import java.util.*

interface MessageRepository : MongoRepository<Message, String> {
    fun findByConversationIdOrderBySentAtDesc(conversationId: String, pageable: Pageable): Page<Message>

    fun findByConversationId(conversationId: String): List<Message>

    fun findByConversationIdAndReadIdsNotContains(conversationId: String, userId: String): List<Message>

    fun countByConversationIdAndReadIdsNotContains(conversationId: String, userId: String): Int

    fun deleteByConversationId(conversationId: String)

    //các method cho thread
    fun findByThreadIdOrderBySentAtAsc(threadId: String, pageable: Pageable): Page<Message>
    fun findByThreadIdOrderBySentAtDesc(threadId: String, pageable: Pageable): Page<Message>
    fun countByThreadId(threadId: String): Long

    @Aggregation(pipeline = [
        """{
            ${"$"}match: {
              ${"$"}text: {
                ${"$"}search: ?0
              }
            }
        }""",
        """{
            ${"$"}match: {
              "type": ?1
            }
        }"""
    ])
    fun findByContent(keyword: String, type: MessageType): MutableList<Message>

    @Aggregation(pipeline = [
        """{
            ${"$"}match: {
              "attachments.name": ?0
            }
        }""",
        """{
            ${"$"}match: {
              "type": ?1
            }
        }"""
    ])
    fun findByAttachmentsName(keyword: String, type: MessageType): MutableList<Message>

    @Aggregation(pipeline = [
        """{
            ${"$"}match:
            {
                "attachments._id": ObjectId(?0),
            },
        }""",
        """{
            ${"$"}unwind:
            {
                path: "${"$"}attachments",
                preserveNullAndEmptyArrays: true,
            },
        }""",
        """{
            ${"$"}match:
            {
                "attachments._id": ObjectId(?0),
            },
        }""",
        """{
            ${"$"}replaceRoot:
            {
                newRoot: "${"$"}attachments",
            },
        }""",
    ])
    fun findAttachmentById(attachmentId: String): Optional<Attachment>

    // Message status tracking methods
    @Query("{'conversationId': ?0, 'fromUserId': {\$ne: ?1}, 'sentAt': {\$gte: ?2}, 'deliveredIds': {\$nin: [?1]}}")
    fun findRecentUndeliveredMessages(
        conversationId: String,
        userId: String,
        since: LocalDateTime
    ): List<Message>

    @Query("{'conversationId': ?0, 'fromUserId': {\$ne: ?1}, 'readIds': {\$nin: [?1]}}")
    fun findUnreadMessagesForUser(conversationId: String, userId: String): List<Message>

    @Query("{'conversationId': ?0, 'fromUserId': {\$ne: ?1}, 'deliveredIds': {\$nin: [?1]}}")
    fun findUndeliveredMessagesForUser(conversationId: String, userId: String): List<Message>

    @Query("{'conversationId': ?0, 'sentAt': {\$gte: ?1}}")
    fun findRecentMessagesByConversation(conversationId: String, since: LocalDateTime): List<Message>

    // Batch update support
    @Query("{'_id': {\$in: ?0}}")
    fun findMessagesByIds(messageIds: List<String>): List<Message>

    // Statistics queries
    @Aggregation(pipeline = [
        """{
            ${"$"}match: {
                "conversationId": ?0,
                "fromUserId": ?1,
                "sentAt": {
                    ${"$"}gte: ?2
                }
            }
        }""",
        """{
            ${"$"}group: {
                "_id": null,
                "totalSent": { ${"$"}sum: 1 },
                "totalDelivered": { ${"$"}sum: "${"$"}deliveredCount" },
                "totalRead": { ${"$"}sum: "${"$"}readCount" }
            }
        }"""
    ])
    fun getMessageStatistics(conversationId: String, userId: String, since: LocalDateTime): MessageStatistics?

    // For large group optimization
    @Query("{'conversationId': ?0, 'fromUserId': ?1, 'sentAt': {\$gte: ?2}}")
    fun findUserRecentMessages(conversationId: String, userId: String, since: LocalDateTime): List<Message>

    /**
     * Tìm vị trí của message trong conversation (sorted by sentAt desc)
     * Return số lượng messages newer than target message
     */
    @Query("{'conversationId': ?0, 'sentAt': {\$gt: ?1}}")
    fun countMessagesNewerThan(conversationId: String, targetMessageSentAt: LocalDateTime): Long

    /**
     * Lấy messages xung quanh target message với context
     */
    @Query("{'conversationId': ?0}")
    fun findByConversationIdWithContext(
        conversationId: String,
        pageable: Pageable
    ): Page<Message>

    /**
     * Tìm message và lấy context xung quanh nó
     */
    @Aggregation(pipeline = [
        """
        {
            ${'$'}match: {
                conversationId: ?0
            }
        }
        """,
        """
        {
            ${'$'}sort: {
                sentAt: -1
            }
        }
        """,
        """
        {
            ${'$'}group: {
                _id: null,
                messages: { ${'$'}push: "$${'$'}ROOT" },
                total: { ${'$'}sum: 1 }
            }
        }
        """,
        """
        {
            ${'$'}project: {
                targetIndex: {
                    ${'$'}indexOfArray: ["${'$'}messages._id", ?1]
                },
                messages: 1,
                total: 1
            }
        }
        """
    ])
    fun findMessagePositionInConversation(conversationId: String, messageId: String): MessagePositionResult?

    fun findByConversationIdAndSentAtGreaterThan(
        conversationId: String,
        sentAt: LocalDateTime,
        pageable: Pageable
    ): Page<Message>

    fun findByConversationIdAndSentAtLessThan(
        conversationId: String,
        sentAt: LocalDateTime,
        pageable: Pageable
    ): Page<Message>
}

data class MessageStatistics(
    val totalSent: Int,
    val totalDelivered: Int,
    val totalRead: Int
)

data class MessagePositionResult(
    val targetIndex: Int,
    val messages: List<Message>,
    val total: Int
)