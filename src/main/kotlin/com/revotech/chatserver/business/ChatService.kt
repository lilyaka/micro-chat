package com.revotech.chatserver.business

import com.revotech.chatserver.business.attachment.Attachment
import com.revotech.chatserver.business.conversation.Conversation
import com.revotech.chatserver.business.conversation.ConversationRepository
import com.revotech.chatserver.business.exception.ConversationNotFoundException
import com.revotech.chatserver.business.exception.MessageException
import com.revotech.chatserver.business.message.Message
import com.revotech.chatserver.business.message.MessageRepository
import com.revotech.chatserver.client.ChatAttachmentPayload
import com.revotech.chatserver.client.FileServiceClient
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.AttachmentPayload
import com.revotech.util.WebUtil
import org.bson.types.ObjectId
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal

@Service
class ChatService(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val fileServiceClient: FileServiceClient,
    private val webUtil: WebUtil,
    private val tenantHelper: TenantHelper,
) {

    fun getConversation(conversationId: String?): Conversation {
        return if (!conversationId.isNullOrEmpty()) {
            conversationRepository.findById(conversationId).orElseThrow {
                ConversationNotFoundException("conversationNotFound", "Conversation not found.")
            }
        } else {
            Conversation()
        }
    }

    fun saveConversation(conversation: Conversation) = conversationRepository.save(conversation)

    fun saveMessage(message: Message) = messageRepository.save(message)

    fun getMessage(messageId: String): Message {
        return messageRepository.findById(messageId).orElseThrow {
            MessageException("messageNotFound", "Message not found.")
        }
    }

    fun countUnreadMessage(conversationId: String, userId: String) =
        messageRepository.countByConversationIdAndReadIdsNotContains(conversationId, userId)

    fun deleteConversationMessages(conversationId: String) = messageRepository.deleteByConversationId(conversationId)

    fun convertAttachments(
        conversationId: String,
        files: MutableList<AttachmentPayload>?,
        principal: Principal,
    ): MutableList<Attachment>? {
        return if (files.isNullOrEmpty()) {
            null
        } else {
            files.map {
                val id = ObjectId().toString()
                Attachment(
                    id,
                    it.name,
                    fileServiceClient.uploadChatAttachment(
                        mutableMapOf(
                            "userId" to principal.name,
                            webUtil.tenantHeaderKey to tenantHelper.getTenantId(principal as AbstractAuthenticationToken)
                        ),
                        ChatAttachmentPayload(conversationId, id, it.name, it.data)
                    ),
                    it.size
                )
            }.toMutableList()
        }
    }
}
