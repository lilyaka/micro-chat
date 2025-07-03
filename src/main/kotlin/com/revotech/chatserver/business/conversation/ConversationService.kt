package com.revotech.chatserver.business.conversation

import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.PRIVATE_CHANNEL_DESTINATION
import com.revotech.chatserver.business.aop.AfterDeleteConversation
import com.revotech.chatserver.business.exception.ConversationValidateException
import com.revotech.chatserver.business.group.GroupService
import com.revotech.chatserver.business.group.UserLevelInGroup
import com.revotech.chatserver.business.message.Message
import com.revotech.chatserver.business.message.MessageType
import com.revotech.chatserver.business.user.User
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.client.FileServiceClient
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.*
import com.revotech.util.StringUtils
import com.revotech.util.WebUtil
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal

@Service
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val userService: UserService,
    private val groupService: GroupService,
    private val chatService: ChatService,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val fileServiceClient: FileServiceClient,
    private val webUtil: WebUtil,
    private val tenantHelper: TenantHelper // ✅ Added TenantHelper
) {
    // ✅ NEED TENANT CONTEXT - Queries database
    fun searchConversation(keyword: String, principal: Principal): MutableList<Conversation> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val searchKeyword = StringUtils.convertAliasReverse(keyword)
            conversationRepository.findByNameRegex(searchKeyword)
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries/Updates database
    fun updateConversationName(conversationId: String, conversationName: String, principal: Principal): Conversation {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val conversation = chatService.getConversation(conversationId)
            conversation.name = conversationName
            conversationRepository.save(conversation)
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getUserConversations(userId: String, principal: Principal): List<Conversation> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val mapUser = HashMap<String, User?>()
            conversationRepository.findUserConversation(userId).map {
                it.unread = chatService.countUnreadMessage(it.id as String, userId)
                setLastMessageSender(mapUser, it)
                if (!it.isGroup) {
                    get1on1Info(mapUser, userId, it)
                }
                it
            }.filter { it.name.isNotEmpty() }.sortedByDescending { it.lastMessage?.sentAt }
        }
    }

    private fun get1on1Info(mapUser: HashMap<String, User?>, userId: String, conversation: Conversation) {
        if (conversation.members.size != 2) {
            return
        }
        val otherUserId = conversation.members.first { it != userId }
        val user = if (mapUser.containsKey(otherUserId)) {
            mapUser[otherUserId]
        } else {
            userService.getUser(otherUserId)
        }

        conversation.name = user?.fullName ?: ""
        conversation.avatar = user?.avatar ?: ""
    }

    private fun setLastMessageSender(mapUser: HashMap<String, User?>, conversation: Conversation) {
        if (conversation.lastMessage == null) {
            return
        }

        val fromId = conversation.lastMessage?.fromUserId
        if (!fromId.isNullOrEmpty() && !mapUser.containsKey(fromId)) {
            val user = userService.getUser(fromId)
            mapUser[fromId] = user
        }
        conversation.lastMessage?.sender = mapUser[fromId]?.fullName ?: ""
    }

    // ✅ NEED TENANT CONTEXT - Creates/Updates database
    fun createConversation(conversationPayload: ConversationPayload, currentUserId: String, principal: Principal): Conversation {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            if (conversationPayload.members.isEmpty()) {
                throw ConversationValidateException("conversationInvalid", "Cần ít nhất 1 người để tạo cuộc trò chuyện.")
            }

            val allMembers = conversationPayload.members.toMutableList().apply {
                if (!contains(currentUserId)) add(currentUserId)
            }

            when (allMembers.size) {
                2 -> {
                    val targetUserId = allMembers.first { it != currentUserId }
                    findOrCreate1on1ConversationInternal(targetUserId, currentUserId)
                }
                else -> {
                    createGroupConversationInternal(conversationPayload, allMembers, currentUserId)
                }
            }
        }
    }

    private fun createGroupConversationInternal(conversationPayload: ConversationPayload, members: MutableList<String>, currentUserId: String): Conversation {
        if (conversationPayload.name.isBlank()) {
            throw ConversationValidateException("groupNameRequired", "Tên nhóm không được để trống.")
        }

        var conversation = Conversation(
            null,
            conversationPayload.name,
            "",
            true,
            currentUserId,
            mutableListOf(currentUserId),
            members
        )

        conversation = conversationRepository.save(conversation)

        members.filter { it != currentUserId }.forEach { memberId ->
            simpMessagingTemplate.convertAndSendToUser(
                memberId,
                PRIVATE_CHANNEL_DESTINATION,
                NewConversationMessage(conversation)
            )
        }

        return conversation
    }

    // ✅ NEED TENANT CONTEXT - Queries/Creates database
    fun create1on1Conversation(userId: String, currentUserId: String, principal: Principal): Conversation {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            findOrCreate1on1ConversationInternal(userId, currentUserId)
        }
    }

    private fun findOrCreate1on1ConversationInternal(userId: String, currentUserId: String): Conversation {
        return conversationRepository.findExisting1on1Conversation(currentUserId, userId).orElseGet {
            val user = userService.getUser(userId)
            var conversation = Conversation(
                null,
                user?.fullName ?: "",
                user?.avatar ?: "",
                false,
                currentUserId,
                mutableListOf(),
                mutableListOf(userId, currentUserId)
            )
            conversation = conversationRepository.save(conversation)

            simpMessagingTemplate.convertAndSendToUser(
                userId,
                PRIVATE_CHANNEL_DESTINATION,
                NewConversationMessage(conversation)
            )

            conversation
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries/Creates database
    fun createGroupConversation(groupId: String, userId: String, principal: Principal): Conversation {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            conversationRepository.findById(groupId).orElseGet {
                val group = groupService.getGroup(groupId)
                var conversation =
                    Conversation(
                        groupId,
                        group?.name ?: "",
                        "",
                        true,
                        userId,
                        group?.users?.filter { it.level == UserLevelInGroup.MANAGE }?.map { it.id }?.toMutableList()
                            ?: mutableListOf(),
                        group?.users?.map { it.id }?.toMutableList() ?: mutableListOf()
                    )
                conversation = conversationRepository.save(conversation)

                conversation.members.filter { it != userId }.forEach {
                    simpMessagingTemplate.convertAndSendToUser(
                        it,
                        PRIVATE_CHANNEL_DESTINATION,
                        NewConversationMessage(conversation)
                    )
                }

                conversation
            }
        }
    }

    fun addActionMessage(conversation: Conversation, conversationAction: ConversationAction, userId: String): Message {
        val message = Message.Builder()
            .fromUserId(userId)
            .conversationId(conversation.id as String)
            .content(conversationAction.name)
            .attachments(null)
            .type(MessageType.ACTION)
            .build()

        return chatService.saveMessage(message)
    }

    // ✅ NEED TENANT CONTEXT - Queries/Updates database
    fun pinConversationMessage(conversationId: String, messageId: String, principal: Principal): Conversation {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val conversation = chatService.getConversation(conversationId)
            val message = chatService.getMessage(messageId)
            conversation.pin = message
            conversationRepository.save(conversation)
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries/Updates database
    fun unpinConversationMessage(conversationId: String, messageId: String, principal: Principal): Conversation {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val conversation = chatService.getConversation(conversationId)
            conversation.pin = null
            conversationRepository.save(conversation)
        }
    }

    fun addConversationMember(conversationId: String, memberIds: MutableList<String>, userId: String): Conversation {
        var conversation = chatService.getConversation(conversationId)

        val filteredMemberIds = memberIds.filter { !conversation.members.contains(it) }
        if (filteredMemberIds.isNotEmpty()) {
            conversation.members.addAll(filteredMemberIds)

            filteredMemberIds.forEach {
                simpMessagingTemplate.convertAndSendToUser(
                    it,
                    PRIVATE_CHANNEL_DESTINATION,
                    AddMemberMessage(conversation)
                )
            }

            conversation = chatService.saveConversation(conversation)

            val message = addActionMessage(conversation, ConversationAction.ADD_MEMBER, userId)
            message.sender = userService.getUser(userId)?.fullName ?: ""
            conversation.lastMessage = message
        }

        return conversation
    }

    fun removeConversationMember(conversationId: String, memberId: String, userId: String): Conversation {
        var conversation = chatService.getConversation(conversationId)
        if (conversation.members.any { it == memberId }) {
            conversation.members.remove(memberId)
            simpMessagingTemplate.convertAndSendToUser(
                memberId,
                PRIVATE_CHANNEL_DESTINATION,
                RemoveMemberMessage(conversation)
            )

            conversation = chatService.saveConversation(conversation)

            val message = addActionMessage(conversation, ConversationAction.REMOVE_MEMBER, userId)
            message.sender = userService.getUser(userId)?.fullName ?: ""
            conversation.lastMessage = message
        }

        return conversation
    }

    @AfterDeleteConversation
    fun deleteConversation(conversationId: String, userId: String): String {
        val conversation = chatService.getConversation(conversationId)

        chatService.deleteConversationMessages(conversationId)
        conversationRepository.deleteById(conversationId)

        conversation.members.forEach {
            simpMessagingTemplate.convertAndSendToUser(
                it,
                PRIVATE_CHANNEL_DESTINATION,
                DeleteConversationMessage(conversation)
            )
        }

        return conversationId
    }

    fun deleteConversationFolder(conversationId: String) =
        fileServiceClient.deleteChatConversation(webUtil.getHeaders(), conversationId)

    // ✅ NEED TENANT CONTEXT - Queries database
    fun getConversationAttachments(conversationId: String, principal: Principal) =
        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            conversationRepository.findConversationAttachments(conversationId)
        }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun findOrCreate1on1Conversation(targetUserId: String, currentUserId: String, principal: Principal): Conversation {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            findOrCreate1on1ConversationInternal(targetUserId, currentUserId)
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun check1on1ConversationExists(targetUserId: String, currentUserId: String, principal: Principal): String? {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            conversationRepository.findExisting1on1Conversation(currentUserId, targetUserId)
                .map { it.id }
                .orElse(null)
        }
    }
}