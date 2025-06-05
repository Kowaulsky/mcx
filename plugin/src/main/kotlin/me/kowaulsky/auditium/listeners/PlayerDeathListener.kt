package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.PlayerDeathLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import java.sql.Timestamp

class PlayerDeathListener : Listener {
    private val playerDeathLogHelper = PlayerDeathLog()

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val cause = event.entity.lastDamageCause?.cause?.name ?: "Unknown"
        val eventData = mapOf(
            "issuer" to player.name,
            "cause" to cause.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            "location" to "${player.location.x.toInt()}, ${player.location.y.toInt()}, ${player.location.z.toInt()}",
            "world" to player.world.name
        )

        val jsonDetails = playerDeathLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, playerDeathLogHelper.actionType, jsonDetails, timestamp)
    }
}