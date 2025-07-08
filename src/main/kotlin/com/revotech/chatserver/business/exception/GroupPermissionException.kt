package com.revotech.chatserver.business.exception

class GroupPermissionException(code: String, message: String) :
    AppException(code, message),
    ValidateException