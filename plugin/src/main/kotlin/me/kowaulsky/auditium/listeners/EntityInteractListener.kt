package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.EntityInteractLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import java.sql.Timestamp

class EntityInteractListener : Listener {
    private val entityInteractLogHelper = EntityInteractLog()

    @EventHandler
    fun onEntityInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked
        val eventData = mapOf(
            "issuer" to player.name,
            "entity_type" to entity.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            "location" to "${entity.location.x.toInt()}, ${entity.location.y.toInt()}, ${entity.location.z.toInt()}",
            "world" to entity.world.name,
            "cancelled" to event.isCancelled.toString()
        )

        val jsonDetails = entityInteractLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, entityInteractLogHelper.actionType, jsonDetails, timestamp)
    }
}