package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.ItemDropLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import java.sql.Timestamp

class ItemDropListener : Listener {
    private val itemDropLogHelper = ItemDropLog()

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        val player = event.player
        val item = event.itemDrop.itemStack
        val eventData = mapOf(
            "issuer" to player.name,
            "item_type" to item.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            "amount" to item.amount.toString(),
            "location" to "${player.location.x.toInt()}, ${player.location.y.toInt()}, ${player.location.z.toInt()}",
            "world" to player.world.name,
            "cancelled" to event.isCancelled.toString()
        )

        val jsonDetails = itemDropLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, itemDropLogHelper.actionType, jsonDetails, timestamp)
    }
}