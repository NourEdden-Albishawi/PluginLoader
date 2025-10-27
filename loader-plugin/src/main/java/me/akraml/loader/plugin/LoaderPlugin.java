package me.akraml.loader.plugin;

import dev.al3mid3x.security.EncryptionUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LoaderPlugin extends JavaPlugin {

    private final List<Plugin> loadedPlugins = new CopyOnWriteArrayList<>();
    private final List<File> tempPluginFiles = new CopyOnWriteArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("loader").setExecutor(new SyncCommand(this));
        getLogger().info("PluginLoader is enabled. Use /loader sync to synchronize plugins.");
    }

    public void runSync() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                getLogger().info("Connecting to backend to download and prepare plugin data...");
                final List<PluginData> backendPlugins = downloadAllPluginData();
                getLogger().info("Successfully downloaded data for " + backendPlugins.size() + " plugins.");

                Bukkit.getScheduler().runTask(this, () -> startSmartSync(backendPlugins));

            } catch (Exception e) {
                getLogger().severe("An error occurred during the async download phase: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });
    }

    private List<PluginData> downloadAllPluginData() throws Exception {
        final String address = getConfig().getString("loader-server.address");
        final int port = getConfig().getInt("loader-server.port");
        final String authToken = getConfig().getString("loader-server.auth-token");

        if (authToken == null || authToken.isEmpty() || authToken.equals("change-this-secret-token")) {
            throw new IOException("Auth token is not configured in config.yml!");
        }

        final List<PluginData> downloadedData = new ArrayList<>();
        try (final Socket socket = new Socket(address, port);
             final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             final DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF(authToken);
            final int pluginCount = in.readInt();
            final EncryptionUtil encryptionUtil = new EncryptionUtil(authToken);

            File tempDir = new File(getDataFolder(), "temp");
            if (!tempDir.exists()) tempDir.mkdirs();

            for (int i = 0; i < pluginCount; i++) {
                final String pluginName = in.readUTF();
                in.readUTF(); // Discard main class

                final int encryptedSize = in.readInt();
                final byte[] encryptedFileBytes = new byte[encryptedSize];
                in.readFully(encryptedFileBytes, 0, encryptedSize);

                final byte[] fileBytes = encryptionUtil.decryptBytes(encryptedFileBytes);
                downloadedData.add(new PluginData(pluginName, fileBytes));
            }
        }
        return downloadedData;
    }

    private void startSmartSync(List<PluginData> backendPlugins) {
        getLogger().info("Calculating differences for smart sync...");

        Map<String, PluginData> backendPluginMap = backendPlugins.stream()
                .collect(Collectors.toMap(p -> p.name.toLowerCase(), Function.identity()));

        List<Plugin> pluginsToUnload = new ArrayList<>();
        for (Plugin loadedPlugin : loadedPlugins) {
            if (!backendPluginMap.containsKey(loadedPlugin.getName().toLowerCase())) {
                pluginsToUnload.add(loadedPlugin);
            }
        }

        List<PluginData> pluginsToLoad = new ArrayList<>();
        for (PluginData backendPlugin : backendPlugins) {
            if (!isPluginLoaded(backendPlugin.name)) {
                pluginsToLoad.add(backendPlugin);
            }
        }

        getLogger().info("Sync plan: Unload " + pluginsToUnload.size() + " plugins, Load " + pluginsToLoad.size() + " plugins.");

        // Create and run the staggered task
        new SyncTask(this, pluginsToUnload, pluginsToLoad).runTaskTimer(this, 1L, 40L); // Start after 1 tick, run every 2 seconds (40 ticks)
    }

    // These methods are now public to be called by SyncTask
    public void loadPluginFromData(String pluginName, byte[] fileBytes) {
        try {
            getLogger().info("Loading plugin: " + pluginName);
            File tempDir = new File(getDataFolder(), "temp");
            if (!tempDir.exists()) tempDir.mkdirs();
            File tempFile = new File(tempDir, pluginName + ".jar");

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileBytes);
            }

            Plugin loadedPlugin = Bukkit.getPluginManager().loadPlugin(tempFile);
            if (loadedPlugin == null) throw new IllegalStateException("loadPlugin() returned null");

            Bukkit.getPluginManager().enablePlugin(loadedPlugin);
            this.loadedPlugins.add(loadedPlugin);
            this.tempPluginFiles.add(tempFile);
            getLogger().info("Successfully loaded and enabled: " + loadedPlugin.getName());
        } catch (Exception e) {
            getLogger().severe("Failed to load plugin from bytes: " + pluginName + ". Error: " + e.getMessage());
        }
    }

    public void unloadPlugin(Plugin plugin) {
        if (plugin != null) {
            getLogger().info("Unloading plugin: " + plugin.getName());
            final PluginManager pluginManager = Bukkit.getPluginManager();
            if (plugin.isEnabled()) {
                pluginManager.disablePlugin(plugin);
            }
            this.loadedPlugins.remove(plugin);

            File fileToDelete = null;
            for (File tempFile : tempPluginFiles) {
                if (tempFile.getName().equalsIgnoreCase(plugin.getName() + ".jar")) {
                    fileToDelete = tempFile;
                    break;
                }
            }
            if (fileToDelete != null) {
                if (fileToDelete.delete()) {
                    tempPluginFiles.remove(fileToDelete);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        for (Plugin p : this.loadedPlugins) {
            if (p != null && p.isEnabled()) pluginManager.disablePlugin(p);
        }
        this.loadedPlugins.clear();
        for (File tempFile : this.tempPluginFiles) {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
        }
        this.tempPluginFiles.clear();
        getLogger().info("PluginLoader disabled and all managed plugins unloaded.");
    }

    private boolean isPluginLoaded(String name) {
        for (Plugin p : this.loadedPlugins) {
            if (p.getName().equalsIgnoreCase(name)) return true;
        }
        return Bukkit.getPluginManager().getPlugin(name) != null;
    }

    public static class PluginData {
        public final String name;
        public final byte[] fileBytes;

        PluginData(String name, byte[] fileBytes) {
            this.name = name;
            this.fileBytes = fileBytes;
        }
    }
}
