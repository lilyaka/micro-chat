package com.revotech.chatserver.business.user

import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.util.StringUtils
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal

@Service
class UserService(
    private val userRepository: UserRepository,
    private val chatService: ChatService,
    private val tenantHelper: TenantHelper // ✅ Added TenantHelper
) {
    fun getUser(userId: String): User? = userRepository.findById(userId).orElse(null)

    // ✅ NEED TENANT CONTEXT - Queries database
    fun searchUser(keyword: String, principal: Principal): MutableList<User> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val searchKeyword = StringUtils.convertAliasReverse(keyword)
            userRepository.findByUsernameRegexAndEmailRegexAndFullNameRegexAndLockedFalse(
                searchKeyword,
                searchKeyword,
                searchKeyword
            )
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getConversationMembers(memberIds: MutableList<String>, principal: Principal) =
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            userRepository.findByIdIn(memberIds)
        }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getConversationMembers(conversationId: String, principal: Principal): MutableList<User> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val conversation = chatService.getConversation(conversationId)
            userRepository.findByIdIn(conversation.members)
        }
    }
}