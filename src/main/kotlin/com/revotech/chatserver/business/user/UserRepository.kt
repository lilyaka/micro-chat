package com.revotech.chatserver.business.user

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface UserRepository : MongoRepository<User, String> {
    @Query("{'locked' : false, \$or : [{'username' : {\$regex : ?0}}, {'email' : {\$regex : ?0}}, {'fullName' : {\$regex : ?0}}]}")
    fun findByUsernameRegexAndEmailRegexAndFullNameRegexAndLockedFalse(
        username: String,
        email: String,
        fullName: String
    ): MutableList<User>

    fun findByIdIn(userIds: MutableList<String>): MutableList<User>
}