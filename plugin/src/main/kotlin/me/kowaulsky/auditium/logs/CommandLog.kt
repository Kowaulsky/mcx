package me.kowaulsky.auditium.logs

import com.fasterxml.jackson.databind.ObjectMapper
import me.kowaulsky.auditium.database.DatabaseManager
import java.util.UUID
import java.util.logging.Logger

open class CommandLog : LogType {
    private val logger: Logger = Logger.getLogger(CommandLog::class.java.name)
    private val objectMapper = ObjectMapper()

    override val actionType: String = "COMMAND"

    override fun log(playerUUID: UUID, details: String) {
        try {
            DatabaseManager.logPlayerAction(playerUUID, actionType, details)
        } catch (e: Exception) {
            logger.severe("Failed to log command for $playerUUID: ${e.message}")
        }
    }

    override fun toJson(eventData: Map<String, Any>): String {
        return try {
            // Ensure command is sanitized to avoid JSON issues
            val sanitizedData = eventData.toMutableMap()
            sanitizedData["command"] = (eventData["command"] as? String)?.replace("\"", "\\\"") ?: ""
            objectMapper.writeValueAsString(sanitizedData)
        } catch (e: Exception) {
            logger.warning("Failed to convert command data to JSON: ${e.message}")
            "{}"
        }
    }

    override fun toSentence(jsonDetails: String): String {
        return try {
            val data = objectMapper.readValue(jsonDetails, Map::class.java) as Map<String, Any>
            val issuer = data["issuer"] as? String ?: "Unknown"
            val command = data["command"] as? String ?: "Unknown command"
            val results = data["results"] as? String ?: ""
            val resultText = if (results.isNotEmpty()) " with result: $results" else ""
            "$issuer executed command: $command$resultText"
        } catch (e: Exception) {
            logger.warning("Failed to parse JSON details: ${e.message}")
            "Invalid command log"
        }
    }
}