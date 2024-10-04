# StreamlabsPlugin

## Features

- **Listens to Streamlabs Socket API** for events such as:
  - **Twitch**: Follows and subscriptions
  - **YouTube**: Follows, subscriptions, and superchats
  - **Streamlabs**: Donations
- **Executes custom commands** in Minecraft based on the received events.

## Requirements

- **Minecraft server**: Spigot or Paper, version 1.21 or higher.
- **Streamlabs Socket Access Token**: Available from your Streamlabs account under API settings.

## Commands and Permissions

### `/streamlabsreload`
- **Description**: Reloads the plugin's configuration and reconnects to the Streamlabs Socket API.
- **Permission**: `streamlabsplugin.reload`
- **Default Permission**: Operators only (non-operators will need the permission set).

## Installation

1. **Download** the latest version of the `StreamlabsPlugin.jar` file.
2. Place the `StreamlabsPlugin.jar` file into your Minecraft server's `plugins` folder.
3. **Start the Minecraft server** to generate the plugin's configuration file (`config.yml`).
4. **Edit `config.yml`** to:
  - Add your **Streamlabs Socket Access Token**.
  - Customize event-handling commands under the `events` section for various platforms.
  - Configure any additional **chat commands** you’d like players to use in Minecraft chat.
5. **Use the command** `/streamlabsreload` in-game or from the server console to reload the configuration after making changes.

---

This plugin enables you to integrate Streamlabs events with Minecraft, allowing for dynamic, event-based interactions!
