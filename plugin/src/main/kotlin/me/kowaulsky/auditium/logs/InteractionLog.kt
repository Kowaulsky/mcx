package me.kowaulsky.auditium.logs

import com.fasterxml.jackson.databind.ObjectMapper
import me.kowaulsky.auditium.database.DatabaseManager
import java.util.UUID
import java.util.logging.Logger

class InteractionLog : LogType {
    private val logger: Logger = Logger.getLogger(InteractionLog::class.java.name)
    private val objectMapper = ObjectMapper()

    override val actionType: String = "INTERACTION"

    override fun log(playerUUID: UUID, details: String) {
        try {
            DatabaseManager.logPlayerAction(playerUUID, actionType, details)
        } catch (e: Exception) {
            logger.severe("Failed to log interaction for $playerUUID: ${e.message}")
        }
    }

    override fun toJson(eventData: Map<String, Any>): String {
        return try {
            val sanitizedData = eventData.toMutableMap()
            sanitizedData["interaction"] = (eventData["interaction"] as? String)?.replace("\"", "\\\"") ?: ""
            sanitizedData["block_type"] = (eventData["block_type"] as? String)?.replace("\"", "\\\"") ?: ""
            sanitizedData["inventory"] = (eventData["inventory"] as? String)?.replace("\"", "\\\"") ?: ""
            objectMapper.writeValueAsString(sanitizedData)
        } catch (e: Exception) {
            logger.warning("Failed to convert interaction data to JSON: ${e.message}")
            "{}"
        }
    }

    override fun toSentence(jsonDetails: String): String {
        return try {
            val data = objectMapper.readValue(jsonDetails, Map::class.java) as Map<String, Any>
            val issuer = data["issuer"] as? String ?: "Unknown"
            val interaction = data["interaction"] as? String ?: "Unknown interaction"
            val blockType = data["block_type"] as? String ?: "Unknown block"
            val location = data["location"] as? String ?: "Unknown location"
            val world = data["world"] as? String ?: "Unknown world"
            val cancelled = data["cancelled"] as? String ?: "false"
            val inventory = data["inventory"] as? String
            val baseSentence = "$issuer $interaction $blockType at $location in $world"
            val inventoryText = if (inventory != null && inventory != "Empty") " (Inventory: $inventory)" else ""
            val cancelledText = if (cancelled == "true") " (Cancelled)" else ""
            "$baseSentence$inventoryText$cancelledText"
        } catch (e: Exception) {
            logger.warning("Failed to parse JSON details: ${e.message}")
            "Invalid interaction log"
        }
    }
}