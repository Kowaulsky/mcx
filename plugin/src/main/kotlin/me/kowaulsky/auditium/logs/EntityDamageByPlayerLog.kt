package me.kowaulsky.auditium.logs

import com.fasterxml.jackson.databind.ObjectMapper
import me.kowaulsky.auditium.database.DatabaseManager
import java.util.UUID
import java.util.logging.Logger

class EntityDamageByPlayerLog : LogType {
    private val logger: Logger = Logger.getLogger(EntityDamageByPlayerLog::class.java.name)
    private val objectMapper = ObjectMapper()

    override val actionType: String = "COMBAT"

    override fun log(playerUUID: UUID, details: String) {
        try {
            DatabaseManager.logPlayerAction(playerUUID, actionType, details)
        } catch (e: Exception) {
            logger.severe("Failed to log entity damage for $playerUUID: ${e.message}")
        }
    }

    override fun toJson(eventData: Map<String, Any>): String {
        return try {
            val sanitizedData = eventData.toMutableMap()
            sanitizedData["entity_type"] = (eventData["entity_type"] as? String)?.replace("\"", "\\\"") ?: ""
            objectMapper.writeValueAsString(sanitizedData)
        } catch (e: Exception) {
            logger.warning("Failed to convert entity damage data to JSON: ${e.message}")
            "{}"
        }
    }

    override fun toSentence(jsonDetails: String): String {
        return try {
            val data = objectMapper.readValue(jsonDetails, Map::class.java) as Map<String, Any>
            val issuer = data["issuer"] as? String ?: "Unknown"
            val entityType = data["entity_type"] as? String ?: "Unknown entity"
            val damage = data["damage"] as? String ?: "0"
            val location = data["location"] as? String ?: "Unknown location"
            val world = data["world"] as? String ?: "Unknown world"
            val cancelled = data["cancelled"] as? String ?: "false"
            val cancelledText = if (cancelled == "true") " (Cancelled)" else ""
            "$issuer dealt $damage damage to $entityType at $location in $world$cancelledText"
        } catch (e: Exception) {
            logger.warning("Failed to parse JSON details: ${e.message}")
            "Invalid entity damage log"
        }
    }
}