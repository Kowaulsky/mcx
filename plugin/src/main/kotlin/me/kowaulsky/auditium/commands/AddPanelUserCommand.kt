package me.kowaulsky.auditium.commands

import me.kowaulsky.auditium.database.DatabaseManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender

class AddPanelUserCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is ConsoleCommandSender) {
            sender.sendMessage("This command can only be run from the console!")
            return true
        }
        if (args.size != 2) {
            sender.sendMessage("Usage: /addpaneluser <username> <password>")
            return false
        }
        val username = args[0]
        val password = args[1]
        if (DatabaseManager.addPanelUser(username, password)) {
            sender.sendMessage("Added panel user: $username")
            return true
        } else {
            sender.sendMessage("Failed to add user: $username (username may already exist)")
            return false
        }
    }
}