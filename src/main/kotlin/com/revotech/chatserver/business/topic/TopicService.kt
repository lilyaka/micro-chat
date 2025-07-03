package com.revotech.chatserver.business.topic

import com.revotech.chatserver.helper.TenantHelper
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal

@Service
class TopicService(
    private val topicRepository: TopicRepository,
    private val tenantHelper: TenantHelper // ✅ Thêm TenantHelper
) {
    fun getTopic(topicId: String): Topic {
        return topicRepository.findById(topicId).orElseThrow {
            TopicException("topicNotFound", "Topic not found.")
        }
    }

    // ✅ CẦN THÊM method với tenant context
    fun getTopic(topicId: String, principal: Principal): Topic {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            topicRepository.findById(topicId).orElseThrow {
                TopicException("topicNotFound", "Topic not found.")
            }
        }
    }
}