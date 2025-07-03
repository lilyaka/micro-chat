package com.revotech.chatserver.business.group

import com.revotech.chatserver.helper.TenantHelper
import com.revotech.util.StringUtils
import com.revotech.util.WebUtil
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.security.Principal

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val webUtil: WebUtil,
    private val tenantHelper: TenantHelper // ✅ Added TenantHelper
) {
    fun getGroup(groupId: String): Group? = groupRepository.findById(groupId).orElse(null)

    // ✅ NEED TENANT CONTEXT - Queries database
    fun searchGroupUserIn(keyword: String, userId: String, principal: Principal): MutableList<Group> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val searchKeyword = StringUtils.convertAliasReverse(keyword)
            groupRepository.findByNameRegexAndIsDeleteFalseAndUserIdIn(searchKeyword, userId)
        }
    }

    // ✅ NEED TENANT CONTEXT - Queries database
    fun searchGroup(keyword: String, principal: Principal): MutableList<Group> {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val searchKeyword = StringUtils.convertAliasReverse(keyword)
            groupRepository.findByNameRegexAndIsDeleteFalse(searchKeyword)
        }
    }
}