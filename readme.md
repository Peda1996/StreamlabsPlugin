## Features

- Listens to Streamlabs Socket API for events such as:
    - Twitch follows and subscriptions
    - YouTube follows, subscriptions, and superchats
    - Streamlabs donations
- Executes custom commands in Minecraft based on the received events.
- Supports custom chat commands that can be triggered through Minecraft chat.

## Requirements

- Minecraft server (spigot / paper) running on version 1.21 or higher.
- A valid Streamlabs socket access token (available from your Streamlabs account).

## Installation

1. Download the latest version of the `StreamlabsPlugin.jar` file.
2. Place the `StreamlabsPlugin.jar` file into your Minecraft server's `plugins` folder.
3. Start the Minecraft server to generate the plugin's configuration file (`config.yml`).
4. Edit the `config.yml` file to add your Streamlabs socket access token and customize the event-handling commands.