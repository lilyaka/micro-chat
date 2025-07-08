package com.revotech.chatserver.business.exception

open class GroupException(code: String, message: String) : AppException(code, message)

class GroupNotFoundException(code: String, message: String) : GroupException(code, message), NotFoundException