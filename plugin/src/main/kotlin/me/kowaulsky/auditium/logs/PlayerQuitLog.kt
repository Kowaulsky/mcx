package me.kowaulsky.auditium.logs

import com.fasterxml.jackson.databind.ObjectMapper
import me.kowaulsky.auditium.database.DatabaseManager
import java.util.UUID
import java.util.logging.Logger

class PlayerQuitLog : LogType {
    private val logger: Logger = Logger.getLogger(PlayerQuitLog::class.java.name)
    private val objectMapper = ObjectMapper()

    override val actionType: String = "PLAYER"

    override fun log(playerUUID: UUID, details: String) {
        try {
            DatabaseManager.logPlayerAction(playerUUID, actionType, details)
        } catch (e: Exception) {
            logger.severe("Failed to log player quit for $playerUUID: ${e.message}")
        }
    }

    override fun toJson(eventData: Map<String, Any>): String {
        return try {
            objectMapper.writeValueAsString(eventData)
        } catch (e: Exception) {
            logger.warning("Failed to convert player quit data to JSON: ${e.message}")
            "{}"
        }
    }

    override fun toSentence(jsonDetails: String): String {
        return try {
            val data = objectMapper.readValue(jsonDetails, Map::class.java) as Map<String, Any>
            val issuer = data["issuer"] as? String ?: "Unknown"
            val location = data["location"] as? String ?: "Unknown location"
            val world = data["world"] as? String ?: "Unknown world"
            "$issuer quit the server at $location in $world"
        } catch (e: Exception) {
            logger.warning("Failed to parse JSON details: ${e.message}")
            "Invalid player quit log"
        }
    }
}