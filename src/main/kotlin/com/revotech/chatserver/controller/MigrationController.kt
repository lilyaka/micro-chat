package com.revotech.chatserver.controller

import com.revotech.chatserver.business.conversation.ConversationRepository
import com.revotech.chatserver.business.group.GroupService
import org.springframework.web.bind.annotation.*

/**
 * Manual Migration Controller - SAFEST OPTION
 *
 * Cách dùng:
 * 1. Deploy code
 * 2. Call API: POST /admin/migrate/conversation-groupid
 * 3. Check logs/response
 * 4. Delete controller sau khi migration xong
 */
@RestController
@RequestMapping("/admin/migrate")
class MigrationController(
    private val conversationRepository: ConversationRepository,
    private val groupService: GroupService
) {

    @PostMapping("/conversation-groupid")
    fun migrateConversationGroupId(): Map<String, Any> {
        println("🔄 Starting MANUAL Conversation-Group migration...")

        val conversations = conversationRepository.findAll()
        var updatedCount = 0
        val results = mutableListOf<String>()

        conversations.forEach { conversation ->
            if (conversation.isGroup && conversation.groupId == null) {
                val group = groupService.getGroup(conversation.id!!)
                if (group != null) {
                    // ✅ FIXED: Directly update groupId field
                    conversation.groupId = conversation.id
                    conversationRepository.save(conversation)
                    updatedCount++
                    val msg = "✅ Updated conversation ${conversation.id} with groupId ${conversation.id}"
                    println(msg)
                    results.add(msg)
                } else {
                    val msg = "⚠️ No group found for conversation ${conversation.id} (${conversation.name})"
                    println(msg)
                    results.add(msg)
                }
            }
        }

        val summary = "🎯 Migration completed: Updated $updatedCount conversations"
        println(summary)

        return mapOf(
            "success" to true,
            "updatedCount" to updatedCount,
            "totalConversations" to conversations.size,
            "summary" to summary,
            "details" to results
        )
    }

    @GetMapping("/conversation-groupid/preview")
    fun previewMigration(): Map<String, Any> {
        val conversations = conversationRepository.findAll()
        val needsMigration = conversations.filter { it.isGroup && it.groupId == null }

        val preview = needsMigration.map { conversation ->
            val group = groupService.getGroup(conversation.id!!)
            mapOf(
                "conversationId" to conversation.id,
                "conversationName" to conversation.name,
                "hasMatchingGroup" to (group != null),
                "groupId" to (group?.id ?: "NOT_FOUND")
            )
        }

        return mapOf(
            "totalConversations" to conversations.size,
            "needsMigration" to needsMigration.size,
            "preview" to preview
        )
    }
}