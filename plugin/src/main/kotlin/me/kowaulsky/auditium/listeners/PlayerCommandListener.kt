package me.kowaulsky.auditium.listeners

import me.kowaulsky.auditium.database.DatabaseManager // Import DatabaseManager
import me.kowaulsky.auditium.logs.CommandLog // Keep this if CommandLog has other utility, or remove if only for log structure
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import java.sql.Timestamp

// No longer needs to extend CommandLog if DatabaseManager handles the logging logic including JSON creation
class PlayerCommandListener : Listener {

    // If you still want to use CommandLog's toJson, you can instantiate it here
    private val commandLogHelper = CommandLog()

    @EventHandler
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        val command = event.message
        val eventData = mapOf(
            "command" to command,
            "issuer" to player.name,
            "results" to "" // Placeholder; extend later if command results are captured
        )
        // Use the helper to convert to JSON
        val jsonDetails = commandLogHelper.toJson(eventData)
        val timestamp = Timestamp(System.currentTimeMillis()) // Capture timestamp here

        // Call DatabaseManager to log to DB and send to web
        // The actionType "COMMAND" is derived from commandLogHelper.actionType
        DatabaseManager.logPlayerAction(player.uniqueId, commandLogHelper.actionType, jsonDetails, timestamp)
    }
}
    