package me.kowaulsky.auditium.commands

import me.kowaulsky.auditium.database.DatabaseManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender

class RemovePanelUserCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is ConsoleCommandSender) {
            sender.sendMessage("This command can only be run from the console!")
            return true
        }
        if (args.size != 1) {
            sender.sendMessage("Usage: /removepaneluser <username>")
            return false
        }
        val username = args[0]
        if (DatabaseManager.removePanelUser(username)) {
            sender.sendMessage("Removed panel user: $username")
            return true
        } else {
            sender.sendMessage("Failed to remove user: $username (user not found)")
            return false
        }
    }
}