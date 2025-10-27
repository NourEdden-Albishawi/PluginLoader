package me.akraml.loader;

import dev.al3mid3x.discovery.PluginDiscoverer;
import dev.al3mid3x.discovery.PluginInfo;
import lombok.Getter;
import me.akraml.loader.server.LoaderServer;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The main class of the loader's backend server.
 */
public class LoaderBackend {

    @Getter
    private static final Logger logger = Logger.getLogger("main");

    public static void main(String... args) throws IOException {
        final long start = System.currentTimeMillis();
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tT] [%4$-3s] %5$s%n");

        if (createDefaultFilesIfNeeded()) {
            return; // Stop server to allow for configuration
        }

        final Yaml yaml = new Yaml();
        Map<String, Object> config;
        try (final FileInputStream inputStream = new FileInputStream("config.yml")) {
            config = yaml.load(inputStream);
            if (config == null) throw new IOException("Config file is empty or invalid.");
        } catch (Exception e) {
            logger.severe("Failed to load or parse config.yml: " + e.getMessage());
            return;
        }

        final Integer port = (Integer) config.get("port");
        if (port == null) {
            logger.severe("port is not configured in config.yml!");
            return;
        }

        final String authToken = (String) config.get("auth-token");
        if (authToken == null || authToken.isEmpty() || authToken.equals("change-this-secret-token")) {
            logger.severe("auth-token is not configured in config.yml! Please change it from the default value.");
            return;
        }

        final Map<String, PluginInfo> pluginRegistry = new ConcurrentHashMap<>();
        final LoaderServer loaderServer = new LoaderServer(port, authToken, pluginRegistry);

        final PluginDiscoverer discoverer = new PluginDiscoverer("injected-plugins");
        pluginRegistry.putAll(discoverer.discoverPlugins());
        logger.info("Initial discovery found " + pluginRegistry.size() + " plugins.");

        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            logger.info("Performing hot-reload scan for plugins...");
            final Map<String, PluginInfo> discoveredPlugins = discoverer.discoverPlugins();
            final Set<String> discoveredPluginNames = discoveredPlugins.keySet().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            // Unload plugins that are no longer present
            pluginRegistry.keySet().removeIf(pluginName -> {
                if (!discoveredPluginNames.contains(pluginName)) {
                    logger.info("Hot-unloaded plugin: " + pluginName);
                    return true;
                }
                return false;
            });

            // Load new plugins and reload updated ones
            for (final Map.Entry<String, PluginInfo> entry : discoveredPlugins.entrySet()) {
                final String name = entry.getKey().toLowerCase();
                final PluginInfo newInfo = entry.getValue();
                final PluginInfo existingInfo = pluginRegistry.get(name);

                if (existingInfo == null) {
                    pluginRegistry.put(name, newInfo);
                    logger.info("Hot-loaded new plugin: " + newInfo.name());
                } else if (existingInfo.file().lastModified() != newInfo.file().lastModified()) {
                    pluginRegistry.put(name, newInfo);
                    logger.info("Hot-reloaded updated plugin: " + newInfo.name());
                }
            }
        }, 30, 30, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
            loaderServer.shutdownServer();
        }));

        logger.info("Loader server successfully initialized in " + (System.currentTimeMillis() - start) + "ms.");
        logger.info("Press Ctrl+C to shut down the server.");

        loaderServer.startListener();
    }

    private static boolean createDefaultFilesIfNeeded() throws IOException {
        final File configFile = new File("config.yml");
        final File pluginDir = new File("injected-plugins");

        if (configFile.exists() && configFile.length() > 0) {
            return false;
        }

        logger.info("Configuration file 'config.yml' not found or is empty. Creating a default one...");

        if (!pluginDir.exists() && pluginDir.mkdir()) {
            logger.info("Successfully created 'injected-plugins' directory.");
        }

        try (final PrintWriter writer = new PrintWriter(new FileWriter(configFile))) {
            writer.println("# Configuration for the Loader Backend Server");
            writer.println();
            writer.println("# The port the server will listen on");
            writer.println("port: 5003");
            writer.println();
            writer.println("# The secret authentication token that clients (LoaderPlugin) must provide");
            writer.println("auth-token: 'change-this-secret-token'");
        }

        logger.info("======================================================================");
        logger.info("Default configuration has been created.");
        logger.info("1. Edit 'config.yml' to set your port and a secure 'auth-token'.");
        logger.info("2. Place your plugin JAR(s) inside the 'injected-plugins' directory.");
        logger.info("Exiting. Please restart the server once you have configured it.");
        logger.info("======================================================================");

        return true;
    }
}