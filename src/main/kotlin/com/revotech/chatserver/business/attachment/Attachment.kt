package com.revotech.chatserver.business.attachment

import org.springframework.data.annotation.Id

class Attachment(
    @Id
    val id: String?,
    val name: String,
    var path: String?,
    var size: Long
) {
    constructor() : this(
        null, "", "", 0
    )
}
