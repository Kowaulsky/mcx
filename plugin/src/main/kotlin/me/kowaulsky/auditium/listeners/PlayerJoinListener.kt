package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.PlayerJoinLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.sql.Timestamp

class PlayerJoinListener : Listener {
    private val playerJoinLogHelper = PlayerJoinLog()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val eventData = mapOf(
            "issuer" to player.name,
            "location" to "${player.location.x.toInt()}, ${player.location.y.toInt()}, ${player.location.z.toInt()}",
            "world" to player.world.name
        )

        val jsonDetails = playerJoinLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, playerJoinLogHelper.actionType, jsonDetails, timestamp)
    }
}