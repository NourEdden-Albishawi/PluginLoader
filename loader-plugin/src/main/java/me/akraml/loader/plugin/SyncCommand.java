package me.akraml.loader.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles the /loader command.
 */
public class SyncCommand implements CommandExecutor {

    private final LoaderPlugin plugin;

    public SyncCommand(LoaderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pluginloader.command.sync")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("sync")) {
            sender.sendMessage(ChatColor.GOLD + "Starting asynchronous sync with the backend server...");
            sender.sendMessage(ChatColor.GRAY + "Check the console for progress.");

            // Run the sync task asynchronously to not freeze the server
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, plugin::runSync);
            return true;
        }

        sender.sendMessage(ChatColor.AQUA + "Usage: /loader sync");
        return true;
    }
}
