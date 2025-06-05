package me.kowaulsky.auditium.logs

import com.fasterxml.jackson.databind.ObjectMapper
import me.kowaulsky.auditium.database.DatabaseManager
import java.util.UUID
import java.util.logging.Logger

class BlockBreakLog : LogType {
    private val logger: Logger = Logger.getLogger(BlockBreakLog::class.java.name)
    private val objectMapper = ObjectMapper()

    override val actionType: String = "BLOCK"

    override fun log(playerUUID: UUID, details: String) {
        try {
            DatabaseManager.logPlayerAction(playerUUID, actionType, details)
        } catch (e: Exception) {
            logger.severe("Failed to log block break for $playerUUID: ${e.message}")
        }
    }

    override fun toJson(eventData: Map<String, Any>): String {
        return try {
            val sanitizedData = eventData.toMutableMap()
            sanitizedData["block_type"] = (eventData["block_type"] as? String)?.replace("\"", "\\\"") ?: ""
            sanitizedData["drops"] = (eventData["drops"] as? String)?.replace("\"", "\\\"") ?: ""
            objectMapper.writeValueAsString(sanitizedData)
        } catch (e: Exception) {
            logger.warning("Failed to convert block break data to JSON: ${e.message}")
            "{}"
        }
    }

    override fun toSentence(jsonDetails: String): String {
        return try {
            val data = objectMapper.readValue(jsonDetails, Map::class.java) as Map<String, Any>
            val issuer = data["issuer"] as? String ?: "Unknown"
            val blockType = data["block_type"] as? String ?: "Unknown block"
            val location = data["location"] as? String ?: "Unknown location"
            val world = data["world"] as? String ?: "Unknown world"
            val drops = data["drops"] as? String ?: "None"
            val cancelled = data["cancelled"] as? String ?: "false"
            val dropsText = if (drops != "None") " (Drops: $drops)" else ""
            val cancelledText = if (cancelled == "true") " (Cancelled)" else ""
            "$issuer broke $blockType at $location in $world$dropsText$cancelledText"
        } catch (e: Exception) {
            logger.warning("Failed to parse JSON details: ${e.message}")
            "Invalid block break log"
        }
    }
}