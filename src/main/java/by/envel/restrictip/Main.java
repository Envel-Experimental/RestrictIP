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
            playerIPs.put(playerName, ip);
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
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("addip")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You are not an operator!");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /addip <playername> <ip>");
                return true;
            }

            String playerName = args[0];
            String ip = args[1];
            playerIPs.put(playerName, ip);
            sender.sendMessage(ChatColor.GREEN + "IP " + ip + " associated with player " + playerName + ".");
            saveConfigFileAsync(); // Save to the configuration file asynchronously
            return true;
        } else if (command.getName().equalsIgnoreCase("reloadip")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You are not an operator!");
                return true;
            }

            sender.sendMessage(ChatColor.GREEN + "Reloading IP configuration...");
            loadConfig();
            sender.sendMessage(ChatColor.GREEN + "IP configuration reloaded.");
            return true;
        } else if (command.getName().equalsIgnoreCase("restrictip")) {
            // Check if the sender is a player and has operator permission
            if (!(sender instanceof Player) || !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You are not an operator!");
                return true;
            }

            // Extract player name and IP from the sender
            Player player = (Player) sender;
            String playerName = player.getName();
            String playerIP = player.getAddress().getHostString();

            // Update the player-IP association and save to the configuration file asynchronously
            playerIPs.put(playerName, playerIP);
            sender.sendMessage(ChatColor.GREEN + "Only you (player " + playerName + ") can join the server with your current IP.");
            saveConfigFileAsync();
            return true;
        } else if (command.getName().equalsIgnoreCase("reloadip")) {
            // Check if the sender has operator permission
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You are not an operator!");
                return true;
            }

            // Inform the sender about the ongoing reload
            sender.sendMessage(ChatColor.GREEN + "Reloading IP configuration...");

            // Reload the configuration file
            loadConfig();

            // Inform the sender about the completion of the reload
            sender.sendMessage(ChatColor.GREEN + "IP configuration reloaded.");
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        String playerIP = event.getAddress().getHostAddress();

        // Check the IP match for the incoming player
        if (playerIPs.containsKey(playerName) && !playerIPs.get(playerName).equals(playerIP)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Your IP does not match the registered IP.");
        }
    }
}
