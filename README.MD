# PluginLoader

This project is a fork of the original [PluginLoader by AkramLZ](https://github.com/AkramLZ/PluginLoader). It has been significantly updated and improved to provide a robust and efficient solution for dynamically managing Minecraft plugins.

## Overview

PluginLoader is a sophisticated system composed of four main modules:

*   **`loader-plugin`**: A Bukkit plugin that runs on a Minecraft server. It communicates with the backend to dynamically load and unload plugins without requiring a server restart.
*   **`loader-backend`**: A standalone Java application that acts as a centralized server for distributing and managing plugins. It serves plugin files to the `loader-plugin` on demand.
*   **`loader-common`**: A shared library containing utility classes, such as `FileUtils`, which are used by both the `loader-plugin` and `loader-backend` to handle file operations.
*   **`loader-injected-plugin`**: An example plugin that is intended to be served by the `loader-backend`, demonstrating the system's capabilities.

## Compatibility

This project requires at least Spigot (or any related fork) 1.13+ and Java version 17+.

## Key Features

*   **Dynamic Plugin Management**: Load, unload, and update plugins on the fly without requiring a server restart.
*   **Centralized Distribution**: Manage all your plugins from a single backend server.
*   **Secure Communication**: The plugin and backend communicate over a secure channel using an authentication token and AES encryption for plugin data.
*   **Robust Hot-Reloading**: The backend automatically detects new, updated (via file modification timestamp), and removed plugins in its `injected-plugins` directory and updates its registry accordingly.
*   **Staggered Synchronization**: The `loader-plugin` loads and unloads plugins in a staggered manner on the Minecraft server's main thread to prevent server freezes and ensure smooth performance.

## How It Works

1.  **Backend Initialization**: The `loader-backend` starts, reads its `config.yml`, and scans the `injected-plugins` directory for plugin JAR files. It uses a `PluginDiscoverer` to parse each plugin's `plugin.yml` and registers the plugin's metadata.
2.  **Client-Server Communication**: The `loader-plugin` on the Minecraft server connects to the `loader-backend` (triggered by `/loader sync` or on server start). The connection is authenticated using a shared secret token.
3.  **Plugin Synchronization**: Once authenticated, the `loader-backend` sends a list of all available plugins to the `loader-plugin`. For each plugin, it transmits the plugin's name, its encrypted main class, and the encrypted plugin JAR file.
4.  **Dynamic Loading/Unloading**: The `loader-plugin` decrypts the received data and performs a "smart sync." It compares the list of plugins from the backend with the plugins it currently has loaded. Plugins no longer on the backend's list are unloaded, and new or updated ones are loaded.

## Usage

### Loader Backend

1.  **Build**: Build the `loader-backend` module using Gradle:
    ```bash
    ./gradlew :loader-backend:shadowJar
    ```
    This will produce a runnable JAR in `loader-backend/build/libs/`.
2.  **Configure**: Edit the `config.yml` file located in the same directory as your `loader-backend.jar`. Set the `port` and a strong `auth-token`.
3.  **Deploy Plugins**: Create a directory named `injected-plugins` in the same location as your `loader-backend.jar`. Place the JAR files of the plugins you wish to distribute (e.g., `loader-injected-plugin-1.0-SNAPSHOT-all.jar`) into this `injected-plugins` directory. Ensure these plugins have an `api-version` specified in their `plugin.yml`.
4.  **Start**: Run the backend server:
    ```bash
    java -jar loader-backend-1.0-BETA-all.jar
    ```
    The backend will automatically discover plugins in the `injected-plugins` directory and be ready to serve them.

### Loader Plugin

1.  **Build**: Build the `loader-plugin` module using Gradle:
    ```bash
    ./gradlew :loader-plugin:shadowJar
    ```
    This will produce a runnable JAR in `loader-plugin/build/libs/`.
2.  **Deploy**: Place the `loader-plugin-1.0-BETA-all.jar` file into your Minecraft server's `plugins` folder.
3.  **Configure**: A `config.yml` will be generated in the `PluginLoader` plugin data folder (`plugins/PluginLoader/config.yml`). Edit this file to match the `address`, `port`, and `auth-token` configured in your `loader-backend`.
4.  **Start Server**: Start your Minecraft server.
5.  **Synchronize**: Once the server is running, use the in-game command `/loader sync` to initiate the synchronization process and load plugins from the backend.

## Disclaimer

This project is provided as an example to demonstrate the theory and implementation of an external plugin loading system for Minecraft servers. While it includes AES encryption for data transfer and an authentication token for client-server communication, it is crucial to understand the inherent security implications of dynamically loading code.

**It is highly recommended to implement additional robust security measures if you intend to use this project in a production or commercial environment.** This includes, but is not limited to, secure network configurations, advanced authentication mechanisms, and thorough validation of all incoming plugin data. Use at your own risk and ensure you fully understand the security model before deployment.
