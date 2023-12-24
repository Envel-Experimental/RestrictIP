package by.envel.restrictip;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin implements Listener {
    private Map<String, String> playerIPs; // Player Name -> IP
    private String restrictedIP; // Restricted IP
    private FileConfiguration config;

    @Override
    public void onEnable() {
        // Initialize the collection to store the association of player names and IPs
        playerIPs = new HashMap<>();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Load the configuration file
        loadConfig();
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

    private void saveConfigFile() {
        // Save player-IP associations to the configuration
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
            saveConfigFile(); // Save to the configuration file
            return true;
        } else if (command.getName().equalsIgnoreCase("restrictip")) {
            if (!(sender instanceof Player) || !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You are not an operator!");
                return true;
            }

            Player player = (Player) sender;
            String playerName = player.getName();
            String playerIP = player.getAddress().getHostString();

            playerIPs.put(playerName, playerIP);
            restrictedIP = playerIP;

            sender.sendMessage(ChatColor.GREEN + "Only you (player " + playerName + ") can join the server with your current IP.");
            saveConfigFile(); // Save to the configuration file
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String playerIP = event.getAddress().getHostAddress();

        // Check the IP match for the incoming player
        if (playerIPs.containsKey(playerName) && !playerIPs.get(playerName).equals(playerIP)) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Your IP does not match the registered IP.");
            return;
        }

        // Check if the IP matches the restricted IP
        if (restrictedIP != null && !restrictedIP.equals(playerIP)) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Access to the server is restricted from your IP.");
        }
    }
}
