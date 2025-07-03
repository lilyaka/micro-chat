package com.revotech.chatserver.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.revotech.chatserver.business.conversation.Conversation
import com.revotech.chatserver.business.exception.ConversationNotFoundException
import com.revotech.chatserver.business.message.MessageService
import com.revotech.chatserver.business.user.User
import com.revotech.chatserver.business.user.UserService
import com.revotech.chatserver.helper.TenantHelper
import com.revotech.util.WebUtil
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.Principal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class ExportHistoryService(
    private val chatService: ChatService,
    private val messageService: MessageService,
    private val userService: UserService,
    private val webUtil: WebUtil,
    private val webApplicationContext: ResourceLoader,
    private val tenantHelper: TenantHelper // ✅ Added TenantHelper
) {
    // ✅ NEED TENANT CONTEXT - Queries database
    fun exportHistory(conversationId: String, principal: Principal): Resource {
        return tenantHelper.changeTenant(principal as AbstractAuthenticationToken) {
            val conversation = chatService.getConversation(conversationId)
            if (conversation.id == null) {
                throw ConversationNotFoundException("conversationNotFound", "Conversation not found.")
            }

            val zipIn = Files.createTempFile(conversation.name, ".zip")
            val fos = FileOutputStream(zipIn.toFile())
            val zipOut = ZipOutputStream(fos)

            val exportFolder = webApplicationContext.getResource("classpath:export").file

            exportFolder.listFiles()?.forEach {
                if (it.name == "js") {
                    val dataFile = exportMessage(conversation, principal)
                    zipFileFolder(dataFile.file, "js${File.separator}data.js", zipOut)
                }
                zipFileFolder(it, it.name, zipOut)
            }

            zipOut.close()
            fos.close()
            FileSystemResource(zipIn)
        }
    }

    private fun exportMessage(conversation: Conversation, principal: Principal): Resource {
        val mapUser = HashMap<String, User?>()
        val histories = messageService.getAllHistory(conversation.id!!, principal)

        histories.map {
            val fromId = it.fromUserId
            if (!mapUser.containsKey(fromId)) {
                val user = userService.getUser(fromId)
                mapUser[fromId] = user
            }
            it.sender = mapUser[fromId]?.fullName ?: ""

            if (it.replyMessageId != null) {
                val existMessage = histories.find { mess -> mess.id == it.replyMessageId }
                it.replyMessage = existMessage
            }
        }

        val objectMapper = ObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        val messagesJsonFile = Files.createTempFile("data", ".js")
        messagesJsonFile.toFile().printWriter().use {
            it.println("const conversationName = \"${conversation.name}\";")
            it.println("const userId = \"${webUtil.getUserId()}\";")
            it.println("const downloadUrl = \"${webUtil.getGatewayDomain()}/files/download/\";")
            it.println("const messages = ${objectMapper.writeValueAsString(histories)};")
        }

        return FileSystemResource(messagesJsonFile)
    }

    private fun zipFileFolder(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isDirectory) {
            zipOut.putNextEntry(ZipEntry("$fileName/"))
            zipOut.closeEntry()
            fileToZip.listFiles()?.forEach { zipFileFolder(it, fileName + File.separator + it.name, zipOut) }
            return
        }

        val zipEntry = ZipEntry(fileName)
        zipOut.putNextEntry(zipEntry)
        zipOut.write(fileToZip.readBytes())
    }
}