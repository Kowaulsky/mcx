package me.kowaulsky.auditium.logs

import com.fasterxml.jackson.databind.ObjectMapper
import me.kowaulsky.auditium.database.DatabaseManager
import java.util.UUID
import java.util.logging.Logger

class PlayerCraftLog : LogType {
    private val logger: Logger = Logger.getLogger(PlayerCraftLog::class.java.name)
    private val objectMapper = ObjectMapper()

    override val actionType: String = "INVENTORY"

    override fun log(playerUUID: UUID, details: String) {
        try {
            DatabaseManager.logPlayerAction(playerUUID, actionType, details)
        } catch (e: Exception) {
            logger.severe("Failed to log craft item for $playerUUID: ${e.message}")
        }
    }

    override fun toJson(eventData: Map<String, Any>): String {
        return try {
            val sanitizedData = eventData.toMutableMap()
            sanitizedData["item_type"] = (eventData["item_type"] as? String)?.replace("\"", "\\\"") ?: ""
            objectMapper.writeValueAsString(sanitizedData)
        } catch (e: Exception) {
            logger.warning("Failed to convert craft item data to JSON: ${e.message}")
            "{}"
        }
    }

    override fun toSentence(jsonDetails: String): String {
        return try {
            val data = objectMapper.readValue(jsonDetails, Map::class.java) as Map<String, Any>
            val issuer = data["issuer"] as? String ?: "Unknown"
            val itemType = data["item_type"] as? String ?: "Unknown item"
            val amount = data["amount"] as? String ?: "0"
            val location = data["location"] as? String ?: "Unknown location"
            val world = data["world"] as? String ?: "Unknown world"
            val cancelled = data["cancelled"] as? String ?: "false"
            val cancelledText = if (cancelled == "true") " (Cancelled)" else ""
            "$issuer crafted $amount $itemType at $location in $world$cancelledText"
        } catch (e: Exception) {
            logger.warning("Failed to parse JSON details: ${e.message}")
            "Invalid craft item log"
        }
    }
}