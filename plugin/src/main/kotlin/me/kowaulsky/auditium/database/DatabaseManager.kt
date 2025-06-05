package me.kowaulsky.auditium.database

import me.kowaulsky.auditium.Auditium
import me.kowaulsky.auditium.dto.LogEntryDto
import me.kowaulsky.auditium.dto.PanelUserDto
import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

object DatabaseManager {
    private val logger: Logger = Logger.getLogger(DatabaseManager::class.java.name)
    private var pluginInstance: Auditium? = null // To access WebSender

    // Call this from Auditium's onEnable AFTER webSender is initialized
    fun init(plugin: Auditium) {
        this.pluginInstance = plugin
    }

    fun logPlayerAction(playerUUID: UUID, actionType: String, details: String, timestamp: Timestamp = Timestamp(System.currentTimeMillis())) {
        val actionToLog = LogEntryDto(
            playerUUID = playerUUID.toString(),
            actionType = actionType,
            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply { timeZone = TimeZone.getTimeZone("UTC") }.format(timestamp),
            details = details
        )

        try {
            // Validate actionType against ENUM values
            val validActionTypes = listOf("COMMAND", "MOVEMENT", "INTERACTION", "CHAT", "OTHER")
            if (actionType !in validActionTypes) {
                logger.warning("Invalid action_type '$actionType' for player $playerUUID")
                return
            }

            DatabaseConfig.getDataSource()?.connection?.use { conn: Connection ->
                conn.prepareStatement(
                    "INSERT INTO logs (player_uuid, action_type, timestamp, details) VALUES (?, ?, ?, ?)"
                ).use { stmt ->
                    stmt.setString(1, playerUUID.toString())
                    stmt.setString(2, actionType)
                    stmt.setTimestamp(3, timestamp) // Use the passed or generated timestamp
                    stmt.setString(4, details)
                    stmt.executeUpdate()
                }
                // After successful DB log, send to web panel
                pluginInstance?.webSender?.sendLiveLog(actionToLog)

            } ?: throw IllegalStateException("Database not initialized, cannot log player action")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to log action ($actionType) for $playerUUID: ${e.message}", e)
        }
    }

    fun addPanelUser(username: String, password: String): Boolean {
        try {
            DatabaseConfig.getDataSource()?.connection?.use { conn: Connection ->
                conn.prepareStatement(
                    "INSERT INTO panel_users (username, password_hash) VALUES (?, ?)"
                ).use { stmt ->
                    stmt.setString(1, username)
                    stmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()))
                    return stmt.executeUpdate() > 0
                }
            } ?: return false
        } catch (e: Exception) {
            logger.warning("Failed to add panel user '$username': ${e.message}")
            return false // Likely duplicate username
        }
    }

    fun removePanelUser(username: String): Boolean {
        try {
            DatabaseConfig.getDataSource()?.connection?.use { conn: Connection ->
                conn.prepareStatement("DELETE FROM panel_users WHERE username = ?").use { stmt ->
                    stmt.setString(1, username)
                    return stmt.executeUpdate() > 0
                }
            } ?: return false
        } catch (e: Exception) {
            logger.warning("Failed to remove panel user '$username': ${e.message}")
            return false
        }
    }

    fun cleanOldLogs(daysToKeep: Int): Int {
        try {
            DatabaseConfig.getDataSource()?.connection?.use { conn: Connection ->
                conn.prepareStatement(
                    "DELETE FROM logs WHERE timestamp < NOW() - INTERVAL ? DAY"
                ).use { stmt ->
                    stmt.setInt(1, daysToKeep)
                    return stmt.executeUpdate()
                }
            } ?: throw IllegalStateException("Database not initialized")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to clean old logs: ${e.message}", e)
            throw e // Re-throw to indicate failure to the caller if necessary
        }
    }

    /**
     * Fetches all panel users from the database.
     * @return A list of PanelUserDto objects.
     */
    fun getAllPanelUsers(): List<PanelUserDto> {
        val users = mutableListOf<PanelUserDto>()
        try {
            DatabaseConfig.getDataSource()?.connection?.use { conn ->
                conn.prepareStatement("SELECT username, password_hash FROM panel_users").use { stmt ->
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        users.add(PanelUserDto(rs.getString("username"), rs.getString("password_hash")))
                    }
                }
            } ?: logger.warning("Database not initialized, cannot fetch panel users.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to fetch panel users: ${e.message}", e)
        }
        return users
    }

    /**
     * Fetches all log entries from the database.
     * Consider adding pagination or date range parameters for very large log tables.
     * For now, it fetches all logs.
     * @return A list of LogEntryDto objects.
     */
    fun getAllLogs(limit: Int = 1000): List<LogEntryDto> { // Added a default limit
        val logs = mutableListOf<LogEntryDto>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply { timeZone = TimeZone.getTimeZone("UTC") }
        try {
            DatabaseConfig.getDataSource()?.connection?.use { conn ->
                // Fetching in descending order of timestamp to get recent logs if limited
                conn.prepareStatement("SELECT player_uuid, action_type, timestamp, details FROM logs ORDER BY timestamp DESC LIMIT ?").use { stmt ->
                    stmt.setInt(1, limit)
                    val rs: ResultSet = stmt.executeQuery()
                    while (rs.next()) {
                        val timestamp = rs.getTimestamp("timestamp")
                        logs.add(
                            LogEntryDto(
                                playerUUID = rs.getString("player_uuid"),
                                actionType = rs.getString("action_type"),
                                timestamp = dateFormat.format(timestamp),
                                details = rs.getString("details") ?: "{}" // Ensure details is not null
                            )
                        )
                    }
                }
            } ?: logger.warning("Database not initialized, cannot fetch logs.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to fetch logs: ${e.message}", e)
        }
        return logs.reversed() // Reverse to maintain chronological order if that's preferred for the initial payload
    }
}
    