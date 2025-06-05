package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.ChatLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.sql.Timestamp

class PlayerChatListener : Listener {
    private val chatLogHelper = ChatLog()

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        // Skip if the message is a command (starts with "/")
        if (event.message.startsWith("/")) return

        val player = event.player
        val message = event.message
        val eventData = mapOf(
            "message" to message,
            "issuer" to player.name
        )
        val jsonDetails = chatLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())

        DatabaseManager.logPlayerAction(player.uniqueId, chatLogHelper.actionType, jsonDetails, timestamp)
    }
}