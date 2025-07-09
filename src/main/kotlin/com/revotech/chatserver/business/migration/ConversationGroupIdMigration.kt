package com.revotech.chatserver.business.migration

import com.revotech.chatserver.business.conversation.ConversationRepository
import com.revotech.chatserver.business.group.GroupService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * Migration script để update existing conversations với groupId
 * Chạy 1 lần để sync data
 */
@Component
class ConversationGroupIdMigration(
    private val conversationRepository: ConversationRepository,
    private val groupService: GroupService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        println("🔄 Starting Conversation-Group migration...")

        val conversations = conversationRepository.findAll()
        var updatedCount = 0

        conversations.forEach { conversation ->
            if (conversation.isGroup && conversation.groupId == null) {
                // Cách 1: Nếu conversationId = groupId (từ createGroupConversation)
                val group = groupService.getGroup(conversation.id!!)
                if (group != null) {
                    conversation.groupId = conversation.id
                    conversationRepository.save(conversation)
                    updatedCount++
                    println("✅ Updated conversation ${conversation.id} with groupId ${conversation.id}")
                } else {
                    // Cách 2: Tìm group by name và members (fallback)
                    // TODO: Implement logic tìm group matching nếu cần
                    println("⚠️ No group found for conversation ${conversation.id} (${conversation.name})")
                }
            }
        }

        println("🎯 Migration completed: Updated $updatedCount conversations")
    }
}