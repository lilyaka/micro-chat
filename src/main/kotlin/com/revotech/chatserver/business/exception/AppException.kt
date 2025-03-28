package com.revotech.chatserver.business.exception
abstract class AppException(val code: String, message: String) : RuntimeException(message)

interface ValidateException

interface NotFoundException