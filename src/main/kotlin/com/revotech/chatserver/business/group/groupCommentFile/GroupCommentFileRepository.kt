package com.revotech.chatserver.business.group.groupCommentFile

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface GroupCommentFileRepository : MongoRepository<GroupCommentFile, String> {
    fun findByFileIdOrderBySentAtDesc(fileId: String, pageable: Pageable) : Page<GroupCommentFile>

}