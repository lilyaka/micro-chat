package com.revotech.chatserver.business.message

import com.revotech.chatserver.business.attachment.Attachment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
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
}
