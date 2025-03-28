package com.revotech.chatserver.payload

import com.revotech.chatserver.business.conversation.Conversation

abstract class PrivateChannelMessage(
    val type: PrivateChannelMessageType,
)

data class NewConversationMessage(
    val metadata: Conversation
) : PrivateChannelMessage(PrivateChannelMessageType.NEW_CONVERSATION)

data class AddMemberMessage(
    val metadata: Conversation
) : PrivateChannelMessage(PrivateChannelMessageType.SUBSCRIBE)

data class RemoveMemberMessage(
    val metadata: Conversation
) : PrivateChannelMessage(PrivateChannelMessageType.UNSUBSCRIBE)

data class DeleteConversationMessage(
    val metadata: Conversation
) : PrivateChannelMessage(PrivateChannelMessageType.UNSUBSCRIBE)

enum class PrivateChannelMessageType {
    NEW_CONVERSATION,
    SUBSCRIBE,
    UNSUBSCRIBE,
    DELETE_CONVERSATION
}