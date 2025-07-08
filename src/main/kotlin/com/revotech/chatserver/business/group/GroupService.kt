package com.revotech.chatserver.business.group

import com.revotech.util.StringUtils
import com.revotech.util.WebUtil
import org.springframework.stereotype.Service

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
}