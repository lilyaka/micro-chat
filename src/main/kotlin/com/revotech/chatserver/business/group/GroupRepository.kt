package com.revotech.chatserver.business.group

import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository

interface GroupRepository : MongoRepository<Group, String> {

    fun findByNameRegexAndIsDeleteFalse(name: String): MutableList<Group>

    @Aggregation(
        pipeline = [
            """
                {
                  ${"$"}match: {
                    "name": {
                      ${"$"}regex: ?0
                    },
                    "isDelete": false,
                    "users._id": ObjectId(?1)
                  }
                }
            """
        ]
    )
    fun findByNameRegexAndIsDeleteFalseAndUserIdIn(name: String, userId: String): MutableList<Group>
}