package com.github.peda1996.streamlabsplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StreamlabsReloadCommand implements CommandExecutor {

    private final StreamlabsPlugin plugin;

    public StreamlabsReloadCommand(StreamlabsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("streamlabsplugin.reload")) {
            sender.sendMessage("You do not have permission to execute this command.");
            return true;
        }

        sender.sendMessage("Reloading Streamlabs Plugin configuration...");
        plugin.loadConfigAndConnect();
        sender.sendMessage("Streamlabs Plugin configuration reloaded.");
        return true;
    }
}
