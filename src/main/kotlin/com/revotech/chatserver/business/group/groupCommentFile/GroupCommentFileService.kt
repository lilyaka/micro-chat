package com.revotech.chatserver.business.group.groupCommentFile

import com.revotech.chatserver.business.ChatService
import com.revotech.chatserver.business.GROUP_FILE_DESTINATION
import com.revotech.chatserver.business.user.User
import com.revotech.chatserver.business.user.UserRepository
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.chatserver.payload.GroupCommentFilePayload
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal

@Service
class GroupCommentFileService(
        private val groupCommentFileRepository: GroupCommentFileRepository,
        private val userRepository: UserRepository,
        private val tenantHelper: TenantHelper,
        private val chatService: ChatService,
        private val userService: UserService,
        private val simpMessagingTemplate: SimpMessagingTemplate
) {
    fun getCommentFiles(fileId: String, pageable: Pageable): Page<GroupCommentFile> {
        val mapUser = HashMap<String, User?>()
        return groupCommentFileRepository.findByFileIdOrderBySentAtDesc(fileId, pageable).map {
            getCommentInfo(mapUser, it)
            it
        }
    }

    private fun getCommentInfo(mapUser: HashMap<String, User?>, groupTopicComment: GroupCommentFile) {
        val senderId = groupTopicComment.senderId
        if (!mapUser.containsKey(senderId)) {
            val user = userRepository.findById(groupTopicComment.senderId!!).orElse(null)
            mapUser[senderId!!] = user
        }
        groupTopicComment.avatar = mapUser[senderId]?.avatar ?: ""
        groupTopicComment.sender = mapUser[senderId]?.fullName ?: ""
    }


    fun sendComment(groupTopicCommentPayload: GroupCommentFilePayload, principal: Principal) {
        val userId = principal.name

        tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            groupTopicCommentPayload.run {

                val comment = GroupCommentFile.Builder()
                        .senderId(userId)
                        .fileId(groupTopicCommentPayload.fileId)
                        .content(content)
                        .attachments(chatService.convertAttachments(groupTopicCommentPayload.fileId, files, principal))
                        .replyCommentId(groupTopicCommentPayload.replyCommentId)
                        .build()
                simpMessagingTemplate.convertAndSend(
                        "$GROUP_FILE_DESTINATION/${groupTopicCommentPayload.fileId}",
                        getSentCommentInfo(groupCommentFileRepository.save(comment))
                )
            }
        }
    }

    private fun getSentCommentInfo(groupCommentFile: GroupCommentFile): GroupCommentFile {
        userService.getUser(groupCommentFile.senderId!!)?.run {
            groupCommentFile.sender = fullName
            groupCommentFile.avatar = avatar
        }

        return groupCommentFile
    }
}