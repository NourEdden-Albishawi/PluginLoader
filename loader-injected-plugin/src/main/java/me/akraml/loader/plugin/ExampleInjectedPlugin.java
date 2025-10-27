package me.akraml.loader.plugin;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * This is a standard Spigot plugin used to test the universal plugin loader.
 */
public final class ExampleInjectedPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("===============================================");
        getLogger().info("ExampleInjectedPlugin has been successfully loaded!");
        getLogger().info("This plugin was loaded remotely.");
        getLogger().info("===============================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("ExampleInjectedPlugin has been disabled.");
    }

}
