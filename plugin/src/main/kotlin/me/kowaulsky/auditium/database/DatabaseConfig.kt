package me.kowaulsky.auditium.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.FileConfiguration
import org.mindrot.jbcrypt.BCrypt
import java.sql.DriverManager
import java.util.logging.Logger

object DatabaseConfig {
    private var dataSource: HikariDataSource? = null
    private const val DEFAULT_DB_NAME = "auditium"
    private const val DEFAULT_USERNAME = "root"
    private const val DEFAULT_PASSWORD = ""

    fun init(config: FileConfiguration, logger: Logger) {
        // Read database configuration from config.yml
        val host = config.getString("database.host") ?: "localhost"
        val port = config.getInt("database.port", 3306)
        val database = config.getString("database.name") ?: DEFAULT_DB_NAME
        val username = config.getString("database.username") ?: DEFAULT_USERNAME
        val password = config.getString("database.password") ?: DEFAULT_PASSWORD

        // Step 1: Test MySQL server connection and create database
        val baseJdbcUrl = "jdbc:mysql://$host:$port"
        try {
            logger.info("Attempting to connect to MySQL server at $host:$port with username $username")
            DriverManager.getConnection("$baseJdbcUrl?useSSL=false&allowPublicKeyRetrieval=true", username, password).use { conn ->
                logger.info("Connected to MySQL server successfully.")
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS $database")
                    logger.info("Database '$database' created or already exists.")
                }
            }
        } catch (e: Exception) {
            logger.severe("Failed to connect to MySQL server or create database '$database': ${e.message}")
            logger.severe("Troubleshooting steps:")
            logger.severe("1. Ensure MySQL server is running (e.g., 'mysqladmin -u $username -p status').")
            logger.severe("2. Verify credentials in config.yml (host: $host, port: $port, username: $username).")
            logger.severe("3. Check if the user has permission to create databases (e.g., 'GRANT ALL PRIVILEGES ON *.* TO '$username'@'$host';').")
            logger.severe("4. Ensure MySQL is listening on the correct port and host (check my.cnf or server logs).")
            throw RuntimeException("Database connection failed", e)
        }

        // Step 2: Initialize HikariCP with the full JDBC URL
        val jdbcUrl = "$baseJdbcUrl/$database?useSSL=false&allowPublicKeyRetrieval=true"
        try {
            val hikariConfig = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                driverClassName = "com.mysql.cj.jdbc.Driver"
                this.username = username
                this.password = password
                maximumPoolSize = 10
            }
            dataSource = HikariDataSource(hikariConfig)
            logger.info("HikariCP data source initialized for database '$database'.")
        } catch (e: Exception) {
            logger.severe("Failed to initialize HikariCP: ${e.message}")
            throw RuntimeException("HikariCP setup failed", e)
        }

        // Step 3: Create tables if they don't exist
        try {
            dataSource?.connection?.use { conn ->
                conn.createStatement().use { stmt ->
                    // Create panel_users table
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS panel_users (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(50) UNIQUE,
                            password_hash VARCHAR(255)
                        )
                    """.trimIndent())
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS logs (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            player_uuid CHAR(36) NOT NULL,
                            action_type ENUM('COMMAND', 'MOVEMENT', 'INTERACTION', 'CHAT', 'COMBAT', 'OTHER') NOT NULL,
                            timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            details TEXT,
                            INDEX idx_player_uuid (player_uuid),
                            INDEX idx_action_type (action_type),
                            INDEX idx_timestamp (timestamp)
                        )
                    """.trimIndent())
                    logger.info("Database tables created or already exist.")
                }

                // Step 4: Check for default admin user
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT COUNT(*) FROM panel_users")
                    if (rs.next() && rs.getInt(1) == 0) {
                        val defaultPassword = generateRandomPassword()
                        stmt.executeUpdate("""
                            INSERT INTO panel_users (username, password_hash)
                            VALUES ('admin', '${BCrypt.hashpw(defaultPassword, BCrypt.gensalt())}')
                        """.trimIndent())
                        logger.warning("Created default admin user (username: admin, password: $defaultPassword). Change this immediately using /addpaneluser and /removepaneluser!")
                    }
                }
            } ?: throw RuntimeException("DataSource is null after initialization")
        } catch (e: Exception) {
            logger.severe("Failed to initialize database tables: ${e.message}")
            throw RuntimeException("Database table setup failed", e)
        }
    }

    fun getDataSource(): HikariDataSource? = dataSource

    private fun generateRandomPassword(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        return (1..length).map { chars.random() }.joinToString("")
    }
}