package com.revotech.chatserver.business.group

enum class GroupAction {
    SEND_MESSAGE,
    EDIT_GROUP_INFO,
    PIN_MESSAGE,
    ADD_MEMBERS,
    REMOVE_MEMBERS,
    CHANGE_SETTINGS,
    PROMOTE_TO_ADMIN,
    DEMOTE_FROM_ADMIN,
    DELETE_GROUP
}