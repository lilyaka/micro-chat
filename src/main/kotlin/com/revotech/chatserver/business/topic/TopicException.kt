package com.revotech.chatserver.business.topic

import com.revotech.chatserver.business.exception.AppException
import com.revotech.chatserver.business.exception.NotFoundException

open class TopicException(code: String, message: String) : AppException(code, message)

class TopicNotFoundException(code: String, message: String) : TopicException(code, message),
    NotFoundException
