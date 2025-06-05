package me.kowaulsky.auditium.logs

import com.fasterxml.jackson.databind.ObjectMapper
import me.kowaulsky.auditium.database.DatabaseManager
import java.util.UUID
import java.util.logging.Logger

class PlayerTeleportLog : LogType {
    private val logger: Logger = Logger.getLogger(PlayerTeleportLog::class.java.name)
    private val objectMapper = ObjectMapper()

    override val actionType: String = "OTHER"

    override fun log(playerUUID: UUID, details: String) {
        try {
            DatabaseManager.logPlayerAction(playerUUID, actionType, details)
        } catch (e: Exception) {
            logger.severe("Failed to log player teleport for $playerUUID: ${e.message}")
        }
    }

    override fun toJson(eventData: Map<String, Any>): String {
        return try {
            val sanitizedData = eventData.toMutableMap()
            sanitizedData["cause"] = (eventData["cause"] as? String)?.replace("\"", "\\\"") ?: ""
            objectMapper.writeValueAsString(sanitizedData)
        } catch (e: Exception) {
            logger.warning("Failed to convert player teleport data to JSON: ${e.message}")
            "{}"
        }
    }

    override fun toSentence(jsonDetails: String): String {
        return try {
            val data = objectMapper.readValue(jsonDetails, Map::class.java) as Map<String, Any>
            val issuer = data["issuer"] as? String ?: "Unknown"
            val cause = data["cause"] as? String ?: "Unknown cause"
            val fromLocation = data["from_location"] as? String ?: "Unknown location"
            val toLocation = data["to_location"] as? String ?: "Unknown location"
            val fromWorld = data["from_world"] as? String ?: "Unknown world"
            val toWorld = data["to_world"] as? String ?: "Unknown world"
            val cancelled = data["cancelled"] as? String ?: "false"
            val cancelledText = if (cancelled == "true") " (Cancelled)" else ""
            "$issuer teleported via $cause from $fromLocation in $fromWorld to $toLocation in $toWorld$cancelledText"
        } catch (e: Exception) {
            logger.warning("Failed to parse JSON details: ${e.message}")
            "Invalid player teleport log"
        }
    }
}