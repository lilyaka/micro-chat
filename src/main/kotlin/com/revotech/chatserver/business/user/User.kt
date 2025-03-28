package com.revotech.chatserver.business.user

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

const val DB_USER = "sys_user"

@Document(DB_USER)
data class User(
    @Id
    val id: String,
    val username: String?,
    var fullName: String = "",
    val email: String,
    val avatar: String?,
    val locked: Boolean = false
)