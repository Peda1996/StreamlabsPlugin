package com.github.peda1996.streamlabsplugin;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.*;

public class StreamlabsPlugin extends JavaPlugin {

    private Socket socketClient;
    private final Map<String, Integer> commandIndexMap = new HashMap<>(); // Track command indices for each event type

    @Override
    public void onEnable() {
        // Register the reload command
        getCommand("streamlabsreload").setExecutor(new StreamlabsReloadCommand(this));
        loadConfigAndConnect();
    }

    public void loadConfigAndConnect() {
        saveDefaultConfig();
        reloadConfig();

        // Disconnect existing socket if connected
        if (socketClient != null && socketClient.connected()) {
            socketClient.disconnect();
            getLogger().info("Disconnected from previous Streamlabs Socket API connection.");
        }

        String socketAccessToken = getConfig().getString("socket_access_token");
        String executePolicy = getConfig().getString("execute_policy");

        // Event handlers for platforms like Twitch, YouTube, etc.
        ConfigurationSection eventHandlersSection = getConfig().getConfigurationSection("events");

        // Custom chat commands
        ConfigurationSection chatCommandsSection = getConfig().getConfigurationSection("chat_commands");
        Map<String, String> chatCommands = new HashMap<>();
        if (chatCommandsSection != null) {
            for (String key : chatCommandsSection.getKeys(false)) {
                String value = chatCommandsSection.getString(key);
                if (value != null) {
                    chatCommands.put(key, value);
                } else {
                    getLogger().warning("Chat command value for key " + key + " is null.");
                }
            }
        }

        // Connect to Streamlabs Socket API
        connectToStreamlabsSocket(socketAccessToken, executePolicy, eventHandlersSection, chatCommands);
    }


    private void connectToStreamlabsSocket(String socketAccessToken, String executePolicy, ConfigurationSection eventHandlers, Map<String, String> chatCommands) {
        try {
            IO.Options options = new IO.Options();
            options.transports = new String[]{"websocket"};
            options.query = "token=" + socketAccessToken;

            socketClient = IO.socket("https://sockets.streamlabs.com", options);

            // Log when connected
            socketClient.on(Socket.EVENT_CONNECT, args -> {
                getLogger().info("Connected to Streamlabs Socket API.");
            });

            // Log incoming event
            socketClient.on("event", args -> {
                if (args.length > 0) {
                    try {
                        JSONObject eventData = new JSONObject(args[0].toString());
                        handleEvent(eventData, executePolicy, eventHandlers, chatCommands);
                    } catch (Exception e) {
                        getLogger().severe("Error parsing event data: " + e.getMessage());
                    }
                }
            });

            // Log disconnection
            socketClient.on(Socket.EVENT_DISCONNECT, args -> {
                getLogger().info("Disconnected from Streamlabs Socket API.");
            });


            // Log connection errors
            socketClient.on(Socket.EVENT_CONNECT_ERROR, args -> {
                getLogger().severe("Connection error: " + args[0]);
            });


            // Connect the socket
            socketClient.connect();
        } catch (URISyntaxException e) {
            getLogger().severe("Error connecting to Streamlabs Socket API: " + e.getMessage());
        }
    }

    private void handleEvent(JSONObject eventData, String executePolicy, ConfigurationSection eventHandlers, Map<String, String> chatCommands) {

        // Extract the top-level event type
        String eventType = eventData.optString("type", "");
        String plattform = eventData.optString("for", "");

        ConfigurationSection plattformSection = eventHandlers.getConfigurationSection(plattform);
        if (plattformSection == null) {
            getLogger().info("Unhandled platform type: " + plattform);
            return;
        }

        List<String> commands = plattformSection.getStringList(eventType);
        if (commands.size() <= 0) {
            if (!(eventType.contains("alertPlaying") || eventType.contains("streamlabels")))
                getLogger().info("Unhandled event type: " + eventType + " Platform:" + plattform);
            return;
        }

        // Execute the associated commands with the follower's name
        executeCommands(eventType, commands, executePolicy);
    }

    private void executeCommands(String eventType, List<String> commands, String executePolicy) {
        if (commands.isEmpty()) {
            getLogger().warning("No commands to execute.");
            return;
        }

        switch (executePolicy.toLowerCase()) {
            case "random" -> executeRandomCommand(commands);
            case "all" -> executeAllCommands(commands);
            case "inline" -> executeInlineCommand(eventType, commands);
            default -> getLogger().warning("Invalid execute policy: " + executePolicy);
        }
    }

    private void executeRandomCommand(List<String> commands) {
        Random rand = new Random();
        String command = commands.get(rand.nextInt(commands.size()));
        executeCommand(command);
    }

    private void executeAllCommands(List<String> commands) {
        for (String command : commands) {
            executeCommand(command);
        }
    }

    private void executeInlineCommand(String eventType, List<String> commands) {
        // Get or initialize the command index for this specific event type
        int currentIndex = commandIndexMap.getOrDefault(eventType, 0);

        // Execute the command at the current index
        String command = commands.get(currentIndex);
        executeCommand(command);

        // Update the index, wrapping around if necessary
        currentIndex = (currentIndex + 1) % commands.size();
        commandIndexMap.put(eventType, currentIndex); // Update the index map for this event type
    }

    private void executeCommand(String command) {
        Bukkit.getScheduler().runTask(this, () -> {
            if (command.contains("%player%")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String playerCommand = command.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playerCommand);
                }
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        });
    }

    @Override
    public void onDisable() {
        if (socketClient != null && socketClient.connected()) {
            socketClient.disconnect();
        }
    }
}
