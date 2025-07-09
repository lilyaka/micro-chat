package com.revotech.chatserver.business.group

import com.revotech.util.StringUtils
import com.revotech.util.WebUtil
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val webUtil: WebUtil
) {
    fun getGroup(groupId: String): Group? = groupRepository.findById(groupId).orElse(null)

    fun searchGroupUserIn(keyword: String): MutableList<Group> {
        val searchKeyword = StringUtils.convertAliasReverse(keyword)
        val userId = webUtil.getUserId()
        return groupRepository.findByNameRegexAndIsDeleteFalseAndUserIdIn(searchKeyword, userId)
    }

    fun searchGroup(keyword: String): MutableList<Group> {
        val searchKeyword = StringUtils.convertAliasReverse(keyword)
        return groupRepository.findByNameRegexAndIsDeleteFalse(searchKeyword)
    }

    /**
     * ✅ NEW: Create group with proper role assignment
     */
    fun createGroup(
        groupId: String? = null,
        name: String,
        memberIds: List<String>,
        creatorId: String
    ): Group {
        val currentUserId = creatorId

        // ✅ Create UserInGroup list with proper roles
        val users = memberIds.map { memberId ->
            val level = if (memberId == currentUserId) {
                UserLevelInGroup.MANAGER // Creator = MANAGER
            } else {
                UserLevelInGroup.MEMBER  // Others = MEMBER
            }

            UserInGroup(
                id = memberId,
                level = level,
                fullName = null, // Will be populated by repository/service later
                email = null
            )
        }.toMutableList()

        val group = Group(
            id = groupId ?: "", // MongoDB will generate if empty
            name = name,
            isDelete = false,
            users = users,
            settings = GroupSettings.defaultSettings(),
            createdBy = currentUserId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return groupRepository.save(group)
    }

    /**
     * ✅ NEW: Create group from conversation payload
     */
    fun createGroupFromConversation(
        conversationId: String,
        name: String,
        memberIds: List<String>
    ): Group {
        val currentUserId = webUtil.getUserId()
        return createGroup(
            groupId = conversationId, // ⭐ Same ID as conversation
            name = name,
            memberIds = memberIds,
            creatorId = currentUserId
        )
    }

    /**
     * ✅ NEW: Save group
     */
    fun saveGroup(group: Group): Group = groupRepository.save(group)
}