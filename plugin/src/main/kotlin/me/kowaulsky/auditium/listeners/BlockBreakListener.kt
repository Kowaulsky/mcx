package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.BlockBreakLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import java.sql.Timestamp

class BlockBreakListener : Listener {
    private val blockBreakLogHelper = BlockBreakLog()

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val drops = if (!event.isCancelled) event.block.drops.toList().joinToString(", ") { "${it.type.name.lowercase()}: ${it.amount}" } else "None"
        val eventData = mapOf(
            "issuer" to player.name,
            "block_type" to block.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            "location" to "${block.x}, ${block.y}, ${block.z}",
            "world" to block.world.name,
            "drops" to drops,
            "cancelled" to event.isCancelled.toString()
        )

        val jsonDetails = blockBreakLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, blockBreakLogHelper.actionType, jsonDetails, timestamp)
    }
}