package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.BlockPlaceLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import java.sql.Timestamp

class BlockPlaceListener : Listener {
    private val blockPlaceLogHelper = BlockPlaceLog()

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.blockPlaced
        val eventData = mapOf(
            "issuer" to player.name,
            "block_type" to block.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            "location" to "${block.x}, ${block.y}, ${block.z}",
            "world" to block.world.name,
            "cancelled" to event.isCancelled.toString()
        )

        val jsonDetails = blockPlaceLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, blockPlaceLogHelper.actionType, jsonDetails, timestamp)
    }
}