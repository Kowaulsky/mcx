package me.kowaulsky.auditium.listeners

import org.bukkit.plugin.java.JavaPlugin

object ListenerManager {
    fun register(plugin: JavaPlugin) {
        plugin.server.pluginManager.registerEvents(PlayerCommandListener(), plugin)
        plugin.server.pluginManager.registerEvents(PlayerChatListener(), plugin)
        plugin.server.pluginManager.registerEvents(PlayerInteractionListener(), plugin)
        plugin.server.pluginManager.registerEvents(BlockPlaceListener(), plugin)
        plugin.server.pluginManager.registerEvents(BlockBreakListener(), plugin)
        plugin.server.pluginManager.registerEvents(EntityInteractListener(), plugin)
        plugin.server.pluginManager.registerEvents(ItemDropListener(), plugin)
        plugin.server.pluginManager.registerEvents(PlayerJoinListener(), plugin)
        plugin.server.pluginManager.registerEvents(PlayerQuitListener(), plugin)
        plugin.server.pluginManager.registerEvents(PlayerDeathListener(), plugin)
        plugin.server.pluginManager.registerEvents(PlayerCraftListener(), plugin)
        plugin.server.pluginManager.registerEvents(PlayerTeleportListener(), plugin)
        plugin.server.pluginManager.registerEvents(EntityDamageByPlayerListener(), plugin)
    }
}