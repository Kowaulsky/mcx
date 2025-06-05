package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.PlayerTeleportLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent
import java.sql.Timestamp

class PlayerTeleportListener : Listener {
    private val playerTeleportLogHelper = PlayerTeleportLog()

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        val cause = event.cause.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        val from = event.from
        val to = event.to ?: return
        val eventData = mapOf(
            "issuer" to player.name,
            "cause" to cause,
            "from_location" to "${from.x.toInt()}, ${from.y.toInt()}, ${from.z.toInt()}",
            "to_location" to "${to.x.toInt()}, ${to.y.toInt()}, ${to.z.toInt()}",
            "from_world" to (from.world?.name ?: "Unknown"),
            "to_world" to (to.world?.name ?: "Unknown"),
            "cancelled" to event.isCancelled.toString(),
            "subsection" to "Player"
        )

        val jsonDetails = playerTeleportLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, "OTHER", jsonDetails, timestamp)
    }
}