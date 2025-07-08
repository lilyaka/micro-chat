package com.revotech.chatserver.business.user

import com.revotech.chatserver.business.ChatService
import com.revotech.util.StringUtils
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val chatService: ChatService
) {
    fun getUser(userId: String): User? = userRepository.findById(userId).orElse(null)

    fun searchUser(keyword: String): MutableList<User> {
        val searchKeyword = StringUtils.convertAliasReverse(keyword)
        return userRepository.findByUsernameRegexAndEmailRegexAndFullNameRegexAndLockedFalse(
            searchKeyword,
            searchKeyword,
            searchKeyword
        )
    }

    fun getConversationMembers(memberIds: MutableList<String>) = userRepository.findByIdIn(memberIds)

    fun getConversationMembers(conversationId: String): MutableList<User> {
        val conversation = chatService.getConversation(conversationId)
        return userRepository.findByIdIn(conversation.members)
    }
}
