package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.EntityDamageByPlayerLog
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import java.sql.Timestamp

class EntityDamageByPlayerListener : Listener {
    private val entityDamageLogHelper = EntityDamageByPlayerLog()

    @EventHandler
    fun onEntityDamageByPlayer(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Player) return
        val entity = event.entity
        val eventData = mapOf(
            "issuer" to damager.name,
            "entity_type" to entity.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            "damage" to event.finalDamage.toString(),
            "location" to "${entity.location.x.toInt()}, ${entity.location.y.toInt()}, ${entity.location.z.toInt()}",
            "world" to entity.world.name,
            "cancelled" to event.isCancelled.toString()
        )

        val jsonDetails = entityDamageLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(damager.uniqueId, entityDamageLogHelper.actionType, jsonDetails, timestamp)
    }
}