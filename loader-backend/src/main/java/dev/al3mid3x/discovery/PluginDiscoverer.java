package dev.al3mid3x.discovery;

import me.akraml.loader.LoaderBackend;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Scans a directory for plugin JARs and discovers their information.
 */
public final class PluginDiscoverer {

    private final File pluginDirectory;

    public PluginDiscoverer(String directoryPath) {
        this.pluginDirectory = new File(directoryPath);
    }

    public Map<String, PluginInfo> discoverPlugins() {
        final Map<String, PluginInfo> discoveredPlugins = new HashMap<>();
        if (!pluginDirectory.exists() || !pluginDirectory.isDirectory()) {
            LoaderBackend.getLogger().warning("Plugin directory not found: " + pluginDirectory.getAbsolutePath());
            return discoveredPlugins;
        }

        final File[] files = pluginDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            LoaderBackend.getLogger().info("No plugins found in " + pluginDirectory.getAbsolutePath());
            return discoveredPlugins;
        }

        final Yaml yaml = new Yaml();
        for (final File file : files) {
            try (final JarFile jarFile = new JarFile(file)) {
                final ZipEntry pluginYmlEntry = jarFile.getEntry("plugin.yml");
                if (pluginYmlEntry == null) {
                    LoaderBackend.getLogger().warning("Could not find plugin.yml in " + file.getName() + ", skipping...");
                    continue;
                }

                try (final InputStream inputStream = jarFile.getInputStream(pluginYmlEntry)) {
                    final Map<String, Object> pluginInfoMap = yaml.load(inputStream);
                    final String name = (String) pluginInfoMap.get("name");
                    final String mainClass = (String) pluginInfoMap.get("main");

                    if (name == null || mainClass == null) {
                        LoaderBackend.getLogger().warning("Invalid plugin.yml in " + file.getName() + ": missing 'name' or 'main' key.");
                        continue;
                    }

                    final PluginInfo pluginInfo = new PluginInfo(name, mainClass, file);
                    discoveredPlugins.put(name.toLowerCase(), pluginInfo);
                    LoaderBackend.getLogger().info("Discovered plugin: " + name + " (main: " + mainClass + ")");
                }
            } catch (Exception e) {
                LoaderBackend.getLogger().severe("Failed to process JAR file " + file.getName() + ": " + e.getMessage());
            }
        }
        return discoveredPlugins;
    }
}
