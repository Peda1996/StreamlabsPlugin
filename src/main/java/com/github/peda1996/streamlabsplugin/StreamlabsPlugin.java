package com.github.peda1996.streamlabsplugin;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StreamlabsPlugin extends JavaPlugin {

    private Socket socketClient;
    private int commandIndex = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String socketAccessToken = getConfig().getString("socket_access_token");
        String executePolicy = getConfig().getString("execute_policy");

        // Event handlers for platforms like Twitch, YouTube, etc.
        Map<String, Object> eventHandlers = getConfig().getConfigurationSection("events").getValues(false);

        // Custom chat commands
        Map<String, String> chatCommands = new HashMap<>();
        Map<String, Object> rawChatCommands = getConfig().getConfigurationSection("chat_commands").getValues(false);
        for (Map.Entry<String, Object> entry : rawChatCommands.entrySet()) {
            if (entry.getValue() instanceof String) {
                chatCommands.put(entry.getKey(), (String) entry.getValue());
            } else {
                getLogger().warning("Chat command value for key " + entry.getKey() + " is not a string.");
            }
        }

        // Connect to Streamlabs Socket API
        connectToStreamlabsSocket(socketAccessToken, executePolicy, eventHandlers, chatCommands);
    }

    private void connectToStreamlabsSocket(String socketAccessToken, String executePolicy, Map<String, Object> eventHandlers, Map<String, String> chatCommands) {
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
                getLogger().info("Received Event");
                if (args.length > 0) {
                    JSONObject eventData = (JSONObject) args[0];
                    handleEvent(eventData, executePolicy, eventHandlers, chatCommands);
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

    private void handleEvent(JSONObject eventData, String executePolicy, Map<String, Object> eventHandlers, Map<String, String> chatCommands) {
        try {
            getLogger().info("Received event: " + eventData.toString());

            String eventFor = eventData.optString("for", "");
            String eventType = eventData.optString("type", "");

            // Handle platform-specific events
            if (eventHandlers.containsKey(eventFor)) {
                Map<String, Object> platformEvents = (Map<String, Object>) eventHandlers.get(eventFor);
                if (platformEvents.containsKey(eventType)) {
                    List<String> commands = (List<String>) platformEvents.get(eventType);
                    executeCommands(commands, executePolicy);
                } else {
                    getLogger().info("Unhandled event type: " + eventType);
                }
            } else {
                getLogger().info("Unhandled platform: " + eventFor);
            }

            // Handle custom chat commands from the event's message
            JSONArray messageArray = eventData.optJSONArray("message");
            if (messageArray != null && messageArray.length() > 0) {
                String chatMessage = messageArray.getJSONObject(0).optString("message", "");
                for (Map.Entry<String, String> entry : chatCommands.entrySet()) {
                    String chatCommand = entry.getKey();
                    String command = entry.getValue();
                    if (chatMessage.contains(chatCommand)) {
                        executeCommand(command);
                    }
                }
            }

        } catch (JSONException e) {
            getLogger().severe("JSON Exception: " + e.getMessage());
        }
    }

    private void executeCommands(List<String> commands, String executePolicy) {
        if (commands.isEmpty()) {
            getLogger().warning("No commands to execute.");
            return;
        }

        switch (executePolicy.toLowerCase()) {
            case "random" -> executeRandomCommand(commands);
            case "all" -> executeAllCommands(commands);
            case "inline" -> executeInlineCommand(commands);
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

    private void executeInlineCommand(List<String> commands) {
        if (commandIndex >= commands.size()) {
            commandIndex = 0;
        }
        String command = commands.get(commandIndex);
        executeCommand(command);
        commandIndex++;
    }

    private void executeCommand(String command) {
        if (command.contains("%player%")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerCommand = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playerCommand);
            }
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    @Override
    public void onDisable() {
        if (socketClient != null && socketClient.connected()) {
            socketClient.disconnect();
        }
    }
}
