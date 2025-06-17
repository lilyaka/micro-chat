package com.revotech.chatserver.business.thread

import com.revotech.chatserver.business.exception.AppException
import com.revotech.chatserver.business.exception.NotFoundException

open class ThreadException(code: String, message: String) : AppException(code, message)

class ThreadNotFoundException(code: String, message: String) : ThreadException(code, message), NotFoundException
