package me.akraml.loader.plugin;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * A true staggered worker task that performs one operation per execution
 * to prevent the main thread from freezing.
 */
public class SyncTask extends BukkitRunnable {

    private final LoaderPlugin plugin;
    private final List<Plugin> pluginsToUnload;
    private final List<LoaderPlugin.PluginData> pluginsToLoad;

    public SyncTask(LoaderPlugin plugin, List<Plugin> pluginsToUnload, List<LoaderPlugin.PluginData> pluginsToLoad) {
        this.plugin = plugin;
        this.pluginsToUnload = pluginsToUnload;
        this.pluginsToLoad = pluginsToLoad;
    }

    @Override
    public void run() {
        // Prioritize unloading first
        if (!pluginsToUnload.isEmpty()) {
            Plugin pluginToUnload = pluginsToUnload.remove(0);
            plugin.unloadPlugin(pluginToUnload);
            return; // End this tick's execution
        }

        // Then, handle loading
        if (!pluginsToLoad.isEmpty()) {
            LoaderPlugin.PluginData dataToLoad = pluginsToLoad.remove(0);
            // Correctly call the method with two arguments
            plugin.loadPluginFromData(dataToLoad.name, dataToLoad.fileBytes);
            return; // End this tick's execution
        }

        // If both lists are empty, the job is done.
        plugin.getLogger().info("All sync operations complete.");
        this.cancel();
    }
}
