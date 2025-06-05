package me.kowaulsky.auditium.commands

import me.kowaulsky.auditium.Auditium // Import the main plugin class
import org.bukkit.plugin.java.JavaPlugin

object CommandManager {
    // Pass the plugin instance to register
    fun register(plugin: Auditium) {
        plugin.getCommand("generatelink")?.setExecutor(GenerateLinkCommand(plugin)) // Pass plugin instance
        plugin.getCommand("addpaneluser")?.setExecutor(AddPanelUserCommand())
        plugin.getCommand("removepaneluser")?.setExecutor(RemovePanelUserCommand())
        // Add other commands here if they also need the plugin instance
    }
}
    