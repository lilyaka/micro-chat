package com.revotech.chatserver.business.exception

open class MessageException(code: String, message: String) : AppException(code, message)
class MessageNotFoundException(code: String, message: String) : MessageException(code, message), NotFoundException
class MessageValidateException(code: String, message: String) : MessageException(code, message), ValidateException
