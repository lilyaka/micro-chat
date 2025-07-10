package com.revotech.chatserver.business.group

import com.revotech.chatserver.business.conversation.ConversationRepository
import com.revotech.chatserver.business.user.UserService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ConversationGroupMigrationService(
    private val conversationRepository: ConversationRepository,
    private val groupRepository: GroupRepository,
    private val userService: UserService
) {

    fun migrateGroupConversationsToGroups(): ConversationGroupMigrationResult {
        println("🔄 Starting group conversation to Group entity migration...")

        val allConversations = conversationRepository.findAll()
        val groupConversations = allConversations.filter { it.isGroup }

        var migratedCount = 0
        val details = mutableListOf<String>()

        groupConversations.forEach { conversation ->
            val existingGroup = groupRepository.findById(conversation.id!!).orElse(null)

            if (existingGroup == null) {
                println("  🔧 Creating Group entity for conversation: ${conversation.name} (${conversation.id})")

                // Create Group entity from Conversation
                val users = conversation.members.mapIndexed { index, memberId ->
                    val user = userService.getUser(memberId)
                    val level = when {
                        memberId == conversation.creatorId -> UserLevelInGroup.MANAGER
                        conversation.adminIds.contains(memberId) -> UserLevelInGroup.ADMIN
                        else -> UserLevelInGroup.MEMBER
                    }

                    UserInGroup(
                        id = memberId,
                        level = level,
                        fullName = user?.fullName,
                        email = user?.email
                    )
                }.toMutableList()

                val group = Group(
                    id = conversation.id!!,
                    name = conversation.name,
                    isDelete = false,
                    users = users,
                    settings = GroupSettings.defaultSettings(),
                    createdBy = conversation.creatorId,
                    createdAt = conversation.createdAt,
                    updatedAt = LocalDateTime.now()
                )

                try {
                    groupRepository.save(group)
                    migratedCount++
                    val message = "✅ Created Group entity for conversation: ${conversation.name} (${conversation.id}) with ${users.size} members"
                    println("    $message")
                    details.add(message)
                } catch (e: Exception) {
                    val error = "❌ Failed to create Group for conversation ${conversation.id}: ${e.message}"
                    println("    $error")
                    details.add(error)
                }
            } else {
                println("  ⏭️ Group entity already exists for conversation: ${conversation.name} (${conversation.id})")
            }
        }

        val result = ConversationGroupMigrationResult(
            totalGroupConversations = groupConversations.size,
            migratedCount = migratedCount,
            details = details
        )

        println("✅ Group conversation migration completed: $migratedCount groups created out of ${groupConversations.size} group conversations")
        return result
    }

    fun checkGroupConversationMigrationStatus(): GroupConversationMigrationStatus {
        val allConversations = conversationRepository.findAll()
        val groupConversations = allConversations.filter { it.isGroup }

        val conversationsNeedingGroups = groupConversations.count { conversation ->
            val existingGroup = groupRepository.findById(conversation.id!!).orElse(null)
            existingGroup == null
        }

        return GroupConversationMigrationStatus(
            needsMigration = conversationsNeedingGroups > 0,
            conversationsNeedingGroups = conversationsNeedingGroups,
            totalGroupConversations = groupConversations.size
        )
    }
}

data class ConversationGroupMigrationResult(
    val totalGroupConversations: Int,
    val migratedCount: Int,
    val details: List<String>
)

data class GroupConversationMigrationStatus(
    val needsMigration: Boolean,
    val conversationsNeedingGroups: Int,
    val totalGroupConversations: Int
)