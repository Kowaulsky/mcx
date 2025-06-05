package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.logs.InteractionLog
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import java.sql.Timestamp
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.event.block.Action

data class InteractionDetails(
    val blockType: String,
    val location: String,
    val cancelled: Boolean,
    val inventory: String,
    val world: String,
    val subsection: String
)

class PlayerInteractionListener : Listener {
    private val interactionLogHelper = InteractionLog()

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val block = event.clickedBlock ?: return
        val interactionDetails = getInteractionDetails(block, event)

        val eventData = mapOf(
            "issuer" to player.name,
            "interaction" to "Right Click",
            "block_type" to interactionDetails.blockType,
            "location" to interactionDetails.location,
            "world" to interactionDetails.world,
            "cancelled" to interactionDetails.cancelled.toString(),
            "inventory" to interactionDetails.inventory,
            "subsection" to interactionDetails.subsection
        )

        val jsonDetails = interactionLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis())
        DatabaseManager.logPlayerAction(player.uniqueId, interactionLogHelper.actionType, jsonDetails, timestamp)
    }

    private fun getInteractionDetails(block: Block, event: PlayerInteractEvent): InteractionDetails {
        val blockType = block.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        val location = "${block.x}, ${block.y}, ${block.z}"
        val world = block.world.name
        val cancelled = event.isCancelled

        return when {
            isDoorOrTrap(block) -> InteractionDetails(
                blockType = blockType,
                location = location,
                cancelled = cancelled,
                inventory = "None",
                world = world,
                subsection = "Door/Trap"
            )
            block.state is Container -> {
                val container = block.state as Container
                val inventory = container.inventory.contents
                    .filterNotNull()
                    .joinToString(", ") { "${it.type.name.lowercase()}: ${it.amount}" }
                    .ifEmpty { "Empty" }
                InteractionDetails(
                    blockType = blockType,
                    location = location,
                    cancelled = cancelled,
                    inventory = inventory,
                    world = world,
                    subsection = "Container"
                )
            }
            else -> InteractionDetails(
                blockType = blockType,
                location = location,
                cancelled = cancelled,
                inventory = "None",
                world = world,
                subsection = "Other"
            )
        }
    }

    private fun isDoorOrTrap(block: Block): Boolean {
        val doorTypes = setOf(
            "OAK_DOOR", "SPRUCE_DOOR", "BIRCH_DOOR", "JUNGLE_DOOR", "ACACIA_DOOR",
            "DARK_OAK_DOOR", "MANGROVE_DOOR", "CRIMSON_DOOR", "WARPED_DOOR", "IRON_DOOR"
        )
        val trapTypes = setOf(
            "TRAPDOOR", "IRON_TRAPDOOR"
        )
        return block.type.name in doorTypes || block.type.name in trapTypes
    }
}