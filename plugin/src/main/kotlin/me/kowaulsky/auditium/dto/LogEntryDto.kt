package me.kowaulsky.auditium.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Timestamp // Using java.sql.Timestamp for easy formatting if needed, or convert to ISO string

/**
 * Data Transfer Object for log entries.
 * Used for serializing log data to be sent to the web panel.
 *
 * @property playerUUID The UUID of the player associated with the log.
 * @property actionType The type of action logged (e.g., COMMAND, CHAT).
 * @property timestamp The time the log entry was created.
 * @property details Additional details about the log entry, typically in JSON format.
 */
data class LogEntryDto(
    @JsonProperty("player_uuid") val playerUUID: String,
    @JsonProperty("action_type") val actionType: String,
    @JsonProperty("timestamp") val timestamp: String, // Send as ISO 8601 String for JS compatibility
    @JsonProperty("details") val details: String
)
    