package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.PlayerQuitLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.sql.Timestamp

class PlayerQuitListener : Listener {
    private val playerQuitLogHelper = PlayerQuitLog()

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val eventData = mapOf(
            "issuer" to player.name,
            "location" to "${player.location.x.toInt()}, ${player.location.y.toInt()}, ${player.location.z.toInt()}",
            "world" to player.world.name
        )

        val jsonDetails = playerQuitLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, playerQuitLogHelper.actionType, jsonDetails, timestamp)
    }
}