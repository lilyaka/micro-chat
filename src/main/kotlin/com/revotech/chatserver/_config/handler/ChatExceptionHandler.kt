package com.revotech.chatserver._config.handler

import com.revotech.chatserver.business.exception.MessageException
import com.revotech.payload.ErrorPayload
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@ControllerAdvice
@RestController
class ChatExceptionHandler {
    private val log = LoggerFactory.getLogger(this::class.java)

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(NullPointerException::class)
    fun handleNullPointerException(e: NullPointerException): String {
        log.error(e.message)
        return "Bad request"
    }

    @ExceptionHandler(MessageException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleUserException(ex: MessageException): ErrorPayload {
        return ErrorPayload(ex.code, ex.message ?: "")
    }
}
