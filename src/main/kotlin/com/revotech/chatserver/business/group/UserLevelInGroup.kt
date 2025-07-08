package com.revotech.chatserver.business.group

enum class UserLevelInGroup {
    MANAGER,   // Trưởng nhóm - có tất cả quyền
    ADMIN,   // Phó nhóm - có tất cả quyền trừ demote admin và delete group
    MEMBER   // Thành viên - quyền hạn chế, có thể được cấp thêm quyền
}