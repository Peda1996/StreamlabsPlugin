package com.github.peda1996.streamlabsplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StreamlabsPlugin extends JavaPlugin {

    private WebSocketClient socketClient;
    private int commandIndex = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String accessToken = getConfig().getString("access_token");
        List<String> subscriptionCommands = getConfig().getStringList("subscription_commands");
        List<String> likeCommands = getConfig().getStringList("like_commands");
        String executePolicy = getConfig().getString("execute_policy");
        Map<String, Object> chatCommands = getConfig().getConfigurationSection("chat_commands").getValues(false);

        // Connect to the Streamlabs Socket API
        connectToStreamlabsSocket(accessToken, subscriptionCommands, likeCommands, executePolicy, chatCommands);
    }

    private void connectToStreamlabsSocket(String accessToken, List<String> subscriptionCommands, List<String> likeCommands, String executePolicy, Map<String, Object> chatCommands) {
        try {
            URI uri = new URI("wss://sockets.streamlabs.com/socket.io/?token=" + accessToken);
            socketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    getLogger().info("Connected to Streamlabs Socket API.");
                }

                @Override
                public void onMessage(String message) {
                    getLogger().info("Received event: " + message);

                    if (message.contains("subscription")) {
                        executeCommands(subscriptionCommands, executePolicy);
                    } else if (message.contains("like")) {
                        executeCommands(likeCommands, executePolicy);
                    }

                    // Check for custom chat commands
                    for (Map.Entry<String, Object> entry : chatCommands.entrySet()) {
                        String chatCommand = entry.getKey();
                        String command = entry.getValue().toString();
                        if (message.contains(chatCommand)) {
                            executeChatCommand(command);
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    getLogger().info("Socket closed: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    getLogger().severe("WebSocket error: " + ex.getMessage());
                }
            };
            socketClient.connect();
        } catch (URISyntaxException e) {
            getLogger().severe("Invalid URI for Streamlabs Socket API: " + e.getMessage());
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

    private void executeChatCommand(String command) {
        executeCommand(command);
    }

    @Override
    public void onDisable() {
        if (socketClient != null && socketClient.isOpen()) {
            socketClient.close();
        }
    }
}
