package com.revotech.chatserver.business.conversation

import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.PRIVATE_CHANNEL_DESTINATION
import com.revotech.chatserver.business.aop.AfterDeleteConversation
import com.revotech.chatserver.business.exception.ConversationValidateException
import com.revotech.chatserver.business.group.GroupPermissionService
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
    private val groupPermissionService: GroupPermissionService
) {

    // ✅ UPDATED: Proper groupId handling
    fun getUserConversations(): List<ConversationDetailResponse> {
        val mapUser = HashMap<String, User?>()
        val userId = webUtil.getUserId()

        return conversationRepository.findUserConversation(userId).map { conversation ->
            conversation.unread = chatService.countUnreadMessage(conversation.id as String, userId)
            setLastMessageSender(mapUser, conversation)

            if (!conversation.isGroup) {
                get1on1Info(mapUser, userId, conversation)
            }

            // ✅ FIXED: Use actual groupId for group lookup
            val actualGroupId = conversation.getActualGroupId()
            val groupSettings = if (actualGroupId != null) {
                groupService.getGroup(actualGroupId)?.settings
            } else null

            val userPermissions = if (actualGroupId != null) {
                groupPermissionService.calculatePermissions(actualGroupId, userId)
            } else null

            // ✅ Convert to response DTO with correct groupId
            ConversationDetailResponse(
                id = conversation.id,
                name = conversation.name,
                avatar = conversation.avatar,
                isGroup = conversation.isGroup,
                members = conversation.members,
                groupId = actualGroupId, // ✅ Return actual group ID
                groupSettings = groupSettings,
                userPermissions = userPermissions,
                totalAttachment = conversation.totalAttachment,
                unread = conversation.unread
            )
        }.filter { it.name.isNotEmpty() }.sortedWith(
            compareByDescending<ConversationDetailResponse> { it.id }
        )
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

    fun searchConversation(keyword: String): MutableList<Conversation> {
        val searchKeyword = StringUtils.convertAliasReverse(keyword)
        return conversationRepository.findByNameRegex(searchKeyword)
    }

    fun updateConversationName(conversationId: String, conversationName: String): Conversation {
        val conversation = chatService.getConversation(conversationId)
        conversation.name = conversationName
        return saveConversation(conversation)
    }

    /**
     * Tạo conversation thông minh:
     * - 2 người: Tự động tạo 1-on-1 (kiểm tra existing trước)
     * - >2 người: Tự động tạo nhóm (creator luôn là admin)
     */
    fun createConversation(conversationPayload: ConversationPayload): Conversation {
        if (conversationPayload.members.isEmpty()) {
            throw ConversationValidateException("conversationInvalid", "Cần ít nhất 1 người để tạo cuộc trò chuyện.")
        }

        val currentUserId = webUtil.getUserId()
        val allMembers = conversationPayload.members.toMutableList().apply {
            if (!contains(currentUserId)) add(currentUserId)
        }

        // Auto-detect conversation type based on member count
        return when (allMembers.size) {
            2 -> {
                val targetUserId = allMembers.first { it != currentUserId }
                findOrCreate1on1Conversation(targetUserId)
            }
            else -> {
                createGroupConversation(conversationPayload, allMembers)
            }
        }
    }

    /**
     * ✅ UPDATED: Create group conversation without linking to existing group
     */
    private fun createGroupConversation(conversationPayload: ConversationPayload, members: MutableList<String>): Conversation {
        if (conversationPayload.name.isBlank()) {
            throw ConversationValidateException("groupNameRequired", "Tên nhóm không được để trống.")
        }

        val currentUserId = webUtil.getUserId()

        // ✅ ENSURE: Creator luôn là admin đầu tiên
        val adminIds = mutableListOf<String>().apply {
            add(currentUserId) // Creator luôn là admin
        }

        var conversation = Conversation(
            null,
            conversationPayload.name,
            "",
            true, // isGroup = true
            currentUserId,
            adminIds,
            members,
            null // ✅ groupId = null for new group conversations
        )

        conversation = saveConversation(conversation)

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
    fun create1on1Conversation(userId: String): Conversation {
        val currentUserId = webUtil.getUserId()

        return conversationRepository.findExisting1on1Conversation(currentUserId, userId).orElseGet {
            val user = userService.getUser(userId)
            var conversation = Conversation(
                null,
                user?.fullName ?: "",
                user?.avatar ?: "",
                false, // isGroup = false
                currentUserId,
                mutableListOf(), // 1-on-1 không cần admin
                mutableListOf(userId, currentUserId),
                null // ✅ groupId = null for 1-on-1
            )
            conversation = saveConversation(conversation)

            simpMessagingTemplate.convertAndSendToUser(
                userId,
                PRIVATE_CHANNEL_DESTINATION,
                NewConversationMessage(conversation)
            )

            conversation
        }
    }

    /**
     * ✅ UPDATED: Create conversation from existing group with proper groupId linking
     */
    fun createGroupConversation(groupId: String): Conversation {
        val userId = webUtil.getUserId()
        return conversationRepository.findById(groupId).orElseGet {
            val group = groupService.getGroup(groupId)

            // ✅ ENSURE: Creator + Group managers đều là admin
            val adminIds = mutableListOf<String>().apply {
                add(userId) // Creator luôn là admin đầu tiên

                // Thêm group managers (nếu chưa có)
                group?.users?.filter { it.level == UserLevelInGroup.MANAGER }
                    ?.map { it.id }
                    ?.forEach { adminId ->
                        if (!contains(adminId)) add(adminId)
                    }
            }

            var conversation = Conversation(
                groupId, // ✅ ConversationId = GroupId for compatibility
                group?.name ?: "",
                "",
                true,
                userId,
                adminIds,
                group?.users?.map { it.id }?.toMutableList() ?: mutableListOf(),
                groupId // ✅ IMPORTANT: Set groupId to link to actual group
            )

            conversation = saveConversation(conversation)

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

    fun addActionMessage(conversation: Conversation, conversationAction: ConversationAction): Message {
        val userId = webUtil.getUserId()
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

        return saveConversation(conversation)
    }

    fun unpinConversationMessage(conversationId: String, messageId: String): Conversation {
        val conversation = chatService.getConversation(conversationId)
        conversation.pin = null

        return saveConversation(conversation)
    }

    fun addConversationMember(conversationId: String, memberIds: MutableList<String>): Conversation {
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

            conversation = saveConversation(conversation)

            val message = addActionMessage(conversation, ConversationAction.ADD_MEMBER)
            val userId = webUtil.getUserId()
            message.sender = userService.getUser(userId)?.fullName ?: ""
            conversation.lastMessage = message
        }

        return conversation
    }

    fun removeConversationMember(conversationId: String, memberId: String): Conversation {
        var conversation = chatService.getConversation(conversationId)
        if (conversation.members.any { it == memberId }) {
            conversation.members.remove(memberId)
            simpMessagingTemplate.convertAndSendToUser(
                memberId,
                PRIVATE_CHANNEL_DESTINATION,
                RemoveMemberMessage(conversation)
            )

            conversation = saveConversation(conversation)

            val message = addActionMessage(conversation, ConversationAction.REMOVE_MEMBER)
            val userId = webUtil.getUserId()
            message.sender = userService.getUser(userId)?.fullName ?: ""
            conversation.lastMessage = message
        }

        return conversation
    }

    @AfterDeleteConversation
    fun deleteConversation(conversationId: String): String {
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

    fun findOrCreate1on1Conversation(targetUserId: String): Conversation {
        val currentUserId = webUtil.getUserId()

        // Check existing conversation
        val existing = conversationRepository.findExisting1on1Conversation(currentUserId, targetUserId)
        if (existing.isPresent) {
            return existing.get()
        }

        // Create new if not exists
        return create1on1Conversation(targetUserId)
    }

    fun check1on1ConversationExists(targetUserId: String): String? {
        val currentUserId = webUtil.getUserId()
        return conversationRepository.findExisting1on1Conversation(currentUserId, targetUserId)
            .map { it.id }
            .orElse(null)
    }

    /**
     * ✅ ADDED: Save conversation với validation
     */
    private fun saveConversation(conversation: Conversation): Conversation {
        // Validate trước khi save
        conversation.validate()
        return conversationRepository.save(conversation)
    }
}