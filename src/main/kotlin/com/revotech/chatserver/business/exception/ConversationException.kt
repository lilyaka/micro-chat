package com.revotech.chatserver.business.exception

open class ConversationException(code: String, message: String) : AppException(code, message)
class ConversationNotFoundException(code: String, message: String) : ConversationException(code, message), NotFoundException
class ConversationValidateException(code: String, message: String) : ConversationException(code, message), ValidateException
