package com.revotech.chatserver.business.conversation

import com.revotech.chatserver.payload.EcmAttachmentPayload
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*

interface ConversationRepository : MongoRepository<Conversation, String> {
    fun findByNameRegex(keyword: String): MutableList<Conversation>

    @Aggregation(
        pipeline = [
            """{
             ${"$"}match: {
              ${"$"}expr: {
                ${"$"}in: [?0, "${"$"}members"]
              }
             }
            }"""
        ]
    )
    fun findUserConversation(userId: String): List<Conversation>

    @Aggregation(
        pipeline = [
            """{
                ${"$"}match: {
                  ${"$"}and: [
                    {
                      "isGroup": false
                    },
                    {
                      "members": {
                        ${"$"}all: ?0
                      }
                    }
                  ],
                }
              }
            """
        ]
    )
    fun findByIsGroupFalseAndMembersContaining(userIds: MutableCollection<String>): Optional<Conversation>

    @Aggregation(
        pipeline = [
            """{
                ${"$"}match:
                  {
                    _id: ObjectId(?0),
                  },
              }""",
            """{
                ${"$"}lookup:
                  {
                    from: "chat_message",
                    let: {
                      conversationObjectId: {
                        ${"$"}toString: "${"$"}_id",
                      },
                    },
                    pipeline: [
                      {
                        ${"$"}match: {
                          ${"$"}expr: {
                            ${"$"}eq: [
                              "${"$"}conversationId",
                              "${"$"}${"$"}conversationObjectId",
                            ],
                          },
                        },
                      },
                    ],
                    as: "messages",
                  },
              }""",
            """{
                ${"$"}project: {
                    "messages.attachments": 1,
                    "messages.sentAt": 1,
                    "messages.fromUserId": 1,
                    _id: 0,
                  }
                }""",
            """{
                ${"$"}unwind:
                  {
                    path: "${"$"}messages",
                    preserveNullAndEmptyArrays: true,
                  },
              }""",
            """{
                ${"$"}match:
                  {
                    "messages.attachments": {
                      ${"$"}exists: true,
                    },
                  },
              }""",
            """{
                ${"$"}lookup:
                  {
                    from: "sys_user",
                    let: {
                      fromUserObjectId: {
                        ${"$"}toObjectId: "${"$"}messages.fromUserId",
                      },
                    },
                    pipeline: [
                      {
                        ${"$"}match: {
                          ${"$"}expr: {
                            ${"$"}eq: [
                              "${"$"}_id",
                              "${"$$"}fromUserObjectId",
                            ],
                          },
                        },
                      },
                    ],
                    as: "messages.users",
                  },
                }""",
            """{
                ${"$"}unwind:
                  {
                    path: "${"$"}messages.attachments",
                    preserveNullAndEmptyArrays: true,
                  },
              }""",
            """{
                ${"$"}unwind:
                  {
                    path: "${"$"}messages.users",
                    preserveNullAndEmptyArrays: true,
                  },
              }""",
            """{
                ${"$"}addFields:
                  {
                    "messages.attachments.sender":
                      "${"$"}messages.users.fullName",
                    "messages.attachments.sentAt":
                      "${"$"}messages.sentAt",
                  },
                }""",
            """{
                ${"$"}replaceRoot:
                  {
                    newRoot: "${"$"}messages.attachments",
                  },
              }""",
        ]
    )
    fun findConversationAttachments(conversationId: String): List<EcmAttachmentPayload>

    @Aggregation(
        pipeline = [
            """{
            ${"$"}match: {
                "isGroup": false,
                "members": {
                    ${"$"}size: 2,
                    ${"$"}all: [?0, ?1]
                }
            }
        }""",
            """{
            ${"$"}limit: 1
        }"""
        ]
    )
    fun findExisting1on1Conversation(userId1: String, userId2: String): Optional<Conversation>

}
