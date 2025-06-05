package me.kowaulsky.auditium

import me.kowaulsky.auditium.commands.CommandManager
import me.kowaulsky.auditium.database.DatabaseConfig
import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.listeners.ListenerManager
import me.kowaulsky.auditium.web.WebSender
import org.bukkit.plugin.java.JavaPlugin

class Auditium : JavaPlugin() {

    lateinit var webSender: WebSender
        private set // Allow access from within the plugin but not modification from outside

    override fun onEnable() {
        // Save default config.yml
        saveDefaultConfig()

        // Initialize WebSender (reads URL from config)
        // Must be initialized after saveDefaultConfig() and before it's used by DatabaseManager or Commands
        webSender = WebSender(this)
        DatabaseManager.init(this)

        // Initialize database connection with setup
        try {
            DatabaseConfig.init(config, logger) // DatabaseConfig doesn't need webSender directly
        } catch (e: Exception) {
            logger.severe("Failed to initialize database: ${e.message}")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Register listeners (PlayerCommandListener will need access to webSender via DatabaseManager or a direct reference)
        ListenerManager.register(this) // ListenerManager itself doesn't need webSender

        // Register commands (GenerateLinkCommand will need webSender)
        CommandManager.register(this) // CommandManager will pass 'this' (plugin instance) to commands

        logger.info("Auditium enabled!")
    }

    override fun onDisable() {
        DatabaseConfig.getDataSource()?.close()
        // Potentially clean up OkHttp client if necessary, though it's usually fine
        logger.info("Auditium disabled!")
    }
}
    