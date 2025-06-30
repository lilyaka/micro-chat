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
import com.revotech.chatserver.payload.*
import com.revotech.util.StringUtils
import com.revotech.util.WebUtil
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val userService: UserService,
    private val groupService: GroupService,
    private val chatService: ChatService,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val fileServiceClient: FileServiceClient,
    private val webUtil: WebUtil,
) {
    fun searchConversation(keyword: String): MutableList<Conversation> {
        val searchKeyword = StringUtils.convertAliasReverse(keyword)
        return conversationRepository.findByNameRegex(searchKeyword)
    }

    fun updateConversationName(conversationId: String, conversationName: String): Conversation {
        val conversation = chatService.getConversation(conversationId)
        conversation.name = conversationName
        return conversationRepository.save(conversation)
    }

    fun getUserConversations(userId: String): List<Conversation> {
        val mapUser = HashMap<String, User?>()
        return conversationRepository.findUserConversation(userId).map {
            it.unread = chatService.countUnreadMessage(it.id as String, userId)
            setLastMessageSender(mapUser, it)
            if (!it.isGroup) {
                get1on1Info(mapUser, userId, it)
            }
            it
        }.filter { it.name.isNotEmpty() }.sortedByDescending { it.lastMessage?.sentAt }
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

    /**
     * Tạo conversation thông minh:
     * - 2 người: Tự động tạo 1-on-1 (kiểm tra existing trước)
     * - >2 người: Tự động tạo nhóm
     */
    fun createConversation(conversationPayload: ConversationPayload, currentUserId: String): Conversation {
        if (conversationPayload.members.isEmpty()) {
            throw ConversationValidateException("conversationInvalid", "Cần ít nhất 1 người để tạo cuộc trò chuyện.")
        }

        val allMembers = conversationPayload.members.toMutableList().apply {
            if (!contains(currentUserId)) add(currentUserId)
        }

        // Auto-detect conversation type based on member count
        return when (allMembers.size) {
            2 -> {
                val targetUserId = allMembers.first { it != currentUserId }
                findOrCreate1on1Conversation(targetUserId, currentUserId)
            }
            else -> {
                createGroupConversation(conversationPayload, allMembers, currentUserId)
            }
        }
    }

    private fun createGroupConversation(conversationPayload: ConversationPayload, members: MutableList<String>, currentUserId: String): Conversation {
        if (conversationPayload.name.isBlank()) {
            throw ConversationValidateException("groupNameRequired", "Tên nhóm không được để trống.")
        }

        var conversation = Conversation(
            null,
            conversationPayload.name,
            "",
            true, // isGroup = true
            currentUserId,
            mutableListOf(currentUserId), // Creator làm admin
            members
        )

        conversation = conversationRepository.save(conversation)

        // Notify other members
        members.filter { it != currentUserId }.forEach { memberId ->
            simpMessagingTemplate.convertAndSendToUser(
                memberId,
                PRIVATE_CHANNEL_DESTINATION,
                NewConversationMessage(conversation)
            )
        }

        return conversation
    }

    /**
     * Tạo conversation 1-on-1, kiểm tra existing trước
     */
    fun create1on1Conversation(userId: String, currentUserId: String): Conversation {
        return conversationRepository.findExisting1on1Conversation(currentUserId, userId).orElseGet {
            val user = userService.getUser(userId)
            var conversation = Conversation(
                null,
                user?.fullName ?: "",
                user?.avatar ?: "",
                false, // isGroup = false
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

    /**
     * Tạo conversation từ group có sẵn
     */
    fun createGroupConversation(groupId: String, userId: String): Conversation {
        return conversationRepository.findById(groupId).orElseGet {
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

    fun pinConversationMessage(conversationId: String, messageId: String): Conversation {
        val conversation = chatService.getConversation(conversationId)
        val message = chatService.getMessage(messageId)
        conversation.pin = message

        return conversationRepository.save(conversation)
    }

    fun unpinConversationMessage(conversationId: String, messageId: String): Conversation {
        val conversation = chatService.getConversation(conversationId)
        conversation.pin = null

        return conversationRepository.save(conversation)
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

    fun getConversationAttachments(conversationId: String) =
        conversationRepository.findConversationAttachments(conversationId)

    fun findOrCreate1on1Conversation(targetUserId: String, currentUserId: String): Conversation {
        // Check existing conversation
        val existing = conversationRepository.findExisting1on1Conversation(currentUserId, targetUserId)
        if (existing.isPresent) {
            return existing.get()
        }

        // Create new if not exists
        return create1on1Conversation(targetUserId, currentUserId)
    }

    fun check1on1ConversationExists(targetUserId: String, currentUserId: String): String? {
        return conversationRepository.findExisting1on1Conversation(currentUserId, targetUserId)
            .map { it.id }
            .orElse(null)
    }
}