package com.revotech.chatserver.business._init.index

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.revotech.chatserver.business.message.DB_MESSAGE
import com.revotech.config.multitenant.mongodb.cache.TenantCache
import com.revotech.init.IndexCreator
import org.bson.Document
import org.springframework.stereotype.Component

@Component
class ChatIndexCreator(tenantCache: TenantCache) : IndexCreator() {

    override var dbs: List<MongoDatabase> = tenantCache.tenants.values.map { it.getDatabase() }
    override val collectionName = DB_MESSAGE
    override val mapIndex = mapOf(
        Document("content", "text") to IndexOptions().defaultLanguage("none")
    )
}
