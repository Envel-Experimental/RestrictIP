package by.envel.restrictip;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin implements Listener {
    private Map<String, String> playerIPs; // Player Name -> IP
    private FileConfiguration config;

    @Override
    public void onEnable() {
        // Initialize the collection to store the association of player names and IPs
        playerIPs = new HashMap<>();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Load the configuration file asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                loadConfig();
            }
        }.runTaskAsynchronously(this);
    }

    private void loadConfig() {
        // Create the plugin folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Create the plugin file if it doesn't exist
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }

        // Load the configuration file
        config = YamlConfiguration.loadConfiguration(configFile);

        // Load player-IP associations from the configuration
        for (String playerName : config.getKeys(false)) {
            String ip = config.getString(playerName);
            if (playerName != null && ip != null) {
                playerIPs.put(playerName, ip);
            }
        }
    }

    private void saveConfigFileAsync() {
        // Save player-IP associations to the configuration asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<String, String> entry : playerIPs.entrySet()) {
                    config.set(entry.getKey(), entry.getValue());
                }

                // Save the configuration to file
                try {
                    config.save(new File(getDataFolder(), "config.yml"));
                } catch (IOException e) {
                    getLogger().severe("An error occurred while saving the configuration file:");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("addip")) {
            if (sender != null && !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You are not an operator!");
                return true;
            }

            if (args != null && args.length == 2 && args[0] != null && args[1] != null) {
                String playerName = args[0];
                String ip = args[1];
                if (playerName != null && ip != null) {
                    playerIPs.put(playerName, ip);
                    if (sender != null) {
                        sender.sendMessage(ChatColor.GREEN + "IP " + ip + " associated with player " + playerName + ".");
                        saveConfigFileAsync(); // Save to the configuration file asynchronously
                    }
                } else if (sender != null) {
                    sender.sendMessage(ChatColor.RED + "Invalid player name or IP.");
                }
            } else if (sender != null) {
                sender.sendMessage(ChatColor.RED + "Usage: /addip <playername> <ip>");
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("reloadip")) {
            if (sender != null && !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You are not an operator!");
                return true;
            }

            if (sender != null) {
                sender.sendMessage(ChatColor.GREEN + "Reloading IP configuration...");
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "IP configuration reloaded.");
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("restrictip")) {
            // Check if the sender is a player and has operator permission
            if (sender != null && (sender instanceof Player) && sender.isOp()) {
                // Extract player name and IP from the sender
                Player player = (Player) sender;
                String playerName = player.getName();
                String playerIP = player.getAddress() != null ? player.getAddress().getHostString() : null;

                if (playerName != null && playerIP != null) {
                    // Update the player-IP association and save to the configuration file asynchronously
                    playerIPs.put(playerName, playerIP);
                    if (sender != null) {
                        sender.sendMessage(ChatColor.GREEN + "Only you (player " + playerName + ") can join the server with your current IP.");
                        saveConfigFileAsync();
                    }
                } else if (sender != null) {
                    sender.sendMessage(ChatColor.RED + "Unable to retrieve your player name or IP address.");
                }
            } else if (sender != null) {
                sender.sendMessage(ChatColor.RED + "You are not an operator!");
            }
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        String playerIP = event.getAddress() != null ? event.getAddress().getHostAddress() : null;

        if (playerName != null && playerIP != null && playerIPs.containsKey(playerName) && !playerIPs.get(playerName).equals(playerIP)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Your IP does not match the registered IP.");
        }
    }
}
