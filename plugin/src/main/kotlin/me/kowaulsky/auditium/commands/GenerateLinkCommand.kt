package me.kowaulsky.auditium.commands

import me.kowaulsky.auditium.Auditium
import me.kowaulsky.auditium.database.DatabaseManager
import me.kowaulsky.auditium.dto.WebLinkRequestDto
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit

class GenerateLinkCommand(private val plugin: Auditium) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("auditium.generatelink")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED))
            return true
        }

        sender.sendMessage(Component.text("Generating web link, please wait...", NamedTextColor.YELLOW))

        // Fetch data asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val panelUsers = DatabaseManager.getAllPanelUsers()
                // You might want to add arguments to the command to filter logs (e.g., by player, time range)
                // For now, fetching recent logs (e.g., last 1000)
                val logEntries = DatabaseManager.getAllLogs(limit = 1000) // Default limit in getAllLogs

                if (panelUsers.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        sender.sendMessage(Component.text("Cannot generate link: No panel users found in the database.", NamedTextColor.RED))
                    })
                    return@Runnable
                }
                if (logEntries.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        sender.sendMessage(Component.text("No logs found to generate a link for.", NamedTextColor.GOLD))
                    })
                    // Optionally, still generate a link if users exist but no logs,
                    // or decide if this is an error case. For now, let's allow it.
                }

                val payload = WebLinkRequestDto(users = panelUsers, logs = logEntries)

                plugin.webSender.generateWebLink(payload).thenAcceptAsync { webResponse ->
                    Bukkit.getScheduler().runTask(plugin, Runnable { // Switch back to main thread for Bukkit API
                        if (webResponse?.link != null) {
                            val linkComponent = Component.text("Web link generated successfully: ", NamedTextColor.GREEN)
                                .append(Component.text(webResponse.link, NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.openUrl(webResponse.link)))
                            sender.sendMessage(linkComponent)
                            if (sender is Player) {
                                sender.sendMessage(Component.text("Click the link above to open the log viewer. The link is valid for 24 hours.", NamedTextColor.GRAY))
                            } else {
                                sender.sendMessage(Component.text("Copy the link above to open in a browser. The link is valid for 24 hours.", NamedTextColor.GRAY))
                            }
                        } else {
                            sender.sendMessage(Component.text("Failed to generate web link: ${webResponse?.error ?: "Unknown error from web panel."}", NamedTextColor.RED))
                        }
                    })
                }.exceptionally { ex ->
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        plugin.logger.severe("Exception while generating web link: ${ex.message}")
                        sender.sendMessage(Component.text("An internal error occurred while generating the link. Check server console.", NamedTextColor.RED))
                    })
                    null
                }.orTimeout(30, TimeUnit.SECONDS) // Add a timeout for the web request

            } catch (e: Exception) {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    plugin.logger.severe("Error during generate link command: ${e.message}")
                    e.printStackTrace()
                    sender.sendMessage(Component.text("An unexpected error occurred. Please check the console.", NamedTextColor.RED))
                })
            }
        })
        return true
    }
}
    