package com.revotech.chatserver.business.group

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(using = UserLevelInGroupDeserializer::class)
enum class UserLevelInGroup {
    MANAGER,   // Trưởng nhóm - có tất cả quyền
    MANAGE,    // ✅ LEGACY SUPPORT - same as MANAGER
    ADMIN,     // Phó nhóm - có tất cả quyền trừ demote admin và delete group
    MEMBER;    // Thành viên - quyền hạn chế, có thể được cấp thêm quyền

    companion object {
        /**
         * ✅ MIGRATION SUPPORT: Handle legacy enum values
         */
        fun fromString(value: String?): UserLevelInGroup {
            return when (value?.uppercase()) {
                "MANAGER" -> MANAGER
                "MANAGE" -> MANAGE  // ✅ Legacy support - keep as separate enum
                "ADMIN" -> ADMIN
                "MEMBER" -> MEMBER
                else -> MEMBER // Default fallback
            }
        }

        /**
         * ✅ Get effective permissions (MANAGE = MANAGER for permissions)
         */
        fun getEffectiveLevel(level: UserLevelInGroup): UserLevelInGroup {
            return when (level) {
                MANAGE -> MANAGER // Treat MANAGE as MANAGER for permissions
                else -> level
            }
        }
    }
}

class UserLevelInGroupDeserializer : JsonDeserializer<UserLevelInGroup>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UserLevelInGroup {
        val value = p.text
        return UserLevelInGroup.fromString(value)
    }
}