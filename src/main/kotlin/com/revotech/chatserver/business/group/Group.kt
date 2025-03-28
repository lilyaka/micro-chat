package com.revotech.chatserver.business.group

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

const val DB_GROUP = "sys_group"

@Document(DB_GROUP)
data class Group(
    @Id
    val id: String,
    val name: String,
    val isDelete: Boolean = false,
    val users: MutableList<UserInGroup>
)

data class UserInGroup(
    val id: String,
    val level: UserLevelInGroup?,
    val fullName: String?,
    val email: String?
)