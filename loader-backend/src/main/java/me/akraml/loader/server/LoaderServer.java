package me.akraml.loader.server;

import dev.al3mid3x.discovery.PluginInfo;
import dev.al3mid3x.security.EncryptionUtil;
import me.akraml.loader.LoaderBackend;
import me.akraml.loader.utility.FileUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class LoaderServer {

    private final ServerSocket serverSocket;
    private final String authToken;
    private final EncryptionUtil encryptionUtil;
    private final Map<String, PluginInfo> pluginRegistry;
    private final ExecutorService connectionPool;

    public LoaderServer(final int bindingPort, final String authToken, final Map<String, PluginInfo> pluginRegistry) throws IOException {
        this.serverSocket = new ServerSocket(bindingPort);
        this.authToken = authToken;
        this.encryptionUtil = new EncryptionUtil(authToken);
        this.pluginRegistry = pluginRegistry;
        this.connectionPool = Executors.newCachedThreadPool();
    }

    public void startListener() {
        while (!serverSocket.isClosed()) {
            try {
                final Socket socket = serverSocket.accept();
                connectionPool.submit(() -> handleConnection(socket));
            } catch (SocketException e) {
                if (serverSocket.isClosed()) return;
                LoaderBackend.getLogger().warning("SocketException in listener: " + e.getMessage());
            } catch (IOException e) {
                LoaderBackend.getLogger().severe("An I/O error occurred in the listener: " + e.getMessage());
            }
        }
    }

    private void handleConnection(Socket socket) {
        final String hostname = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        try (Socket s = socket;
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

            LoaderBackend.getLogger().info("Connection from /" + hostname + ". Authenticating...");

            if (!authToken.equals(in.readUTF())) {
                LoaderBackend.getLogger().warning("Authentication failed for /" + hostname + ". Invalid token.");
                return;
            }
            LoaderBackend.getLogger().info("Client /" + hostname + " authenticated successfully.");

            out.writeInt(pluginRegistry.size());

            for (PluginInfo pluginInfo : pluginRegistry.values()) {
                out.writeUTF(pluginInfo.name());
                out.writeUTF(encryptionUtil.encrypt(pluginInfo.mainClass()));

                // Read file to memory, encrypt, then send length-prefixed data
                byte[] fileBytes = FileUtils.toByteArray(pluginInfo.file());
                byte[] encryptedBytes = encryptionUtil.encrypt(fileBytes);

                out.writeInt(encryptedBytes.length);
                out.write(encryptedBytes);
            }
            LoaderBackend.getLogger().info("Finished sending " + pluginRegistry.size() + " plugins to /" + hostname);

        } catch (Exception e) {
            LoaderBackend.getLogger().warning("Error during connection with /" + hostname + ": " + e.getMessage());
        } finally {
            LoaderBackend.getLogger().info("Connection with /" + hostname + " closed.");
        }
    }

    public void shutdownServer() {
        // ... (shutdown logic is fine)
    }
}
