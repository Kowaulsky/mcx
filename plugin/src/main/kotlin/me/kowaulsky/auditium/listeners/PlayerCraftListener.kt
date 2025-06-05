package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.PlayerCraftLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import java.sql.Timestamp

class PlayerCraftListener : Listener {
    private val playerCraftLogHelper = PlayerCraftLog()

    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        val item = event.recipe.result
        val eventData = mapOf(
            "issuer" to player.name,
            "item_type" to item.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            "amount" to item.amount.toString(),
            "location" to "${player.location.x.toInt()}, ${player.location.y.toInt()}, ${player.location.z.toInt()}",
            "world" to player.world.name,
            "cancelled" to event.isCancelled.toString()
        )

        val jsonDetails = playerCraftLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, playerCraftLogHelper.actionType, jsonDetails, timestamp)
    }
}