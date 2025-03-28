package com.revotech.chatserver.business.topicComment

import com.revotech.chatserver.business.exception.AppException
import com.revotech.chatserver.business.exception.NotFoundException

class TopicCommentNotFoundException(code: String, message: String) :
    AppException(code, message),
    NotFoundException {
}
