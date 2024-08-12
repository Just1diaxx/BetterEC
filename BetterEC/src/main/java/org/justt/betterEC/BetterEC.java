package org.justt.betterEC;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BetterEC extends JavaPlugin implements Listener, TabExecutor {

    private File customChestFile;
    private FileConfiguration customChestConfig;

    private static final String ENDER_CHEST_TITLE = "§d§lBetter§5§lEC";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        customChestFile = new File(getDataFolder(), "customEnderChests.yml");
        if (!customChestFile.exists()) {
            try {
                customChestFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        customChestConfig = YamlConfiguration.loadConfiguration(customChestFile);

        // Registra i comandi
        this.getCommand("ec").setExecutor(this);
        this.getCommand("openec").setExecutor(this);
    }

    @Override
    public void onDisable() {
        try {
            customChestConfig.save(customChestFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onEnderOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
            openCustomEnderChest((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onEnderClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ENDER_CHEST_TITLE)) {
            saveCustomEnderChest((Player) event.getPlayer(), event.getInventory());
        }
    }

    private void openCustomEnderChest(Player player) {
        UUID playerUUID = player.getUniqueId();
        Inventory customEnderChest = Bukkit.createInventory(player, 54, ENDER_CHEST_TITLE);

        if (customChestConfig.contains(playerUUID.toString())) {
            for (int i = 0; i < customEnderChest.getSize(); i++) {
                if (customChestConfig.contains(playerUUID.toString() + "." + i)) {
                    customEnderChest.setItem(i, customChestConfig.getItemStack(playerUUID.toString() + "." + i));
                }
            }
        } else {
            Inventory originalEnderChest = player.getEnderChest();
            for (int i = 0; i < originalEnderChest.getSize(); i++) {
                customEnderChest.setItem(i, originalEnderChest.getItem(i));
            }
        }

        player.openInventory(customEnderChest);
    }

    private void saveCustomEnderChest(Player player, Inventory customEnderChest) {
        UUID playerUUID = player.getUniqueId();
        for (int i = 0; i < customEnderChest.getSize(); i++) {
            customChestConfig.set(playerUUID.toString() + "." + i, customEnderChest.getItem(i));
        }

        try {
            customChestConfig.save(customChestFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Inventory originalEnderChest = player.getEnderChest();
        for (int i = 0; i < originalEnderChest.getSize(); i++) {
            originalEnderChest.setItem(i, customEnderChest.getItem(i));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ec")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("better.ec")) {
                    openCustomEnderChest(player);
                    return true;
                }

            } else {
                sender.sendMessage("Only players can use this command.");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("openec")) {
            if (sender.hasPermission("betterec.openec")) {
                if (args.length == 1) {
                    Player targetPlayer = Bukkit.getPlayer(args[0]);

                    if (targetPlayer != null) {
                        openCustomEnderChest(targetPlayer);
                        return true;
                    } else {
                        // Se il giocatore non è online, cerca tra gli OfflinePlayer
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                        if (offlinePlayer.hasPlayedBefore()) {
                            openCustomEnderChestForAdmin(offlinePlayer, (Player) sender);
                            return true;
                        } else {
                            sender.sendMessage("Player not found.");
                            return false;
                        }
                    }
                } else {
                    sender.sendMessage("Usage: /openec <player>");
                    return false;
                }
            } else {
                sender.sendMessage("You do not have permission to use this command.");
                return false;
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete (CommandSender sender, Command command, String alias, String[]args){
        if (command.getName().equalsIgnoreCase("openec") && args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
                }
            return playerNames;
        }
        return null;
    }

    private void openCustomEnderChestForAdmin(OfflinePlayer targetPlayer, Player admin) {
        UUID playerUUID = targetPlayer.getUniqueId();
        Inventory customEnderChest = Bukkit.createInventory(admin, 54, ENDER_CHEST_TITLE + " of " + targetPlayer.getName());

        // Carica l'inventario dal file di configurazione
        if (customChestConfig.contains(playerUUID.toString())) {
            for (int i = 0; i < customEnderChest.getSize(); i++) {
                if (customChestConfig.contains(playerUUID.toString() + "." + i)) {
                    customEnderChest.setItem(i, customChestConfig.getItemStack(playerUUID.toString() + "." + i));
                }
            }
        } else {
            admin.sendMessage("This player has no custom Ender Chest data.");
            return;
        }

        admin.openInventory(customEnderChest);
    }
}


