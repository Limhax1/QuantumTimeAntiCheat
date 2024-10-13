package com.gladurbad.medusa;

import io.github.retrooper.packetevents.PacketEvents;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuantumTimePlugin extends JavaPlugin {

    private static final String BAN_MESSAGE = "&7&m---»--*-------------------------------------*--«----- &8&l[&f&lQ&7&lT&f&lA&8&lC&8&l] &7has removed &c%player% &7from the network for using client modifications &7&m---»--*-------------------------------------*--«-----";
    private static final long COOLDOWN_TIME = 10000; // 10 seconds in milliseconds

    private final Map<UUID, Long> lastAutoBanTime = new HashMap<>();
    private long lastConsoleAutoBanTime = 0L;

    @Override
    public void onLoad() {
        PacketEvents.create(this).getSettings().checkForUpdates(false);
        PacketEvents.get().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.get().init();
        QuantumTimeAC.INSTANCE.start(this);
    }

    @Override
    public void onDisable() {
        PacketEvents.get().terminate();
        QuantumTimeAC.INSTANCE.stop(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("autoban") && sender.hasPermission("qtac.autoban")) {
            long currentTime = System.currentTimeMillis();
            long lastUseTime;

            if (sender instanceof Player) {
                Player player = (Player) sender;
                lastUseTime = lastAutoBanTime.getOrDefault(player.getUniqueId(), 0L);
            } else {
                lastUseTime = lastConsoleAutoBanTime;
            }

            if (currentTime - lastUseTime < COOLDOWN_TIME) {
                sender.sendMessage(ChatColor.RED + "Please wait " + (COOLDOWN_TIME / 1000) + " seconds before using this command again.");
                return true;
            }

            if (args.length >= 1) {
                String playerName = args[0];
                Player targetPlayer = Bukkit.getPlayer(playerName);

                if (targetPlayer != null && !targetPlayer.isBanned()) {
                    String message = BAN_MESSAGE.replace("%player%", playerName);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "say " + playerName + " 3s Unfair Advantage");
                    }, 60L);
                    sender.sendMessage(ChatColor.GREEN + "Broadcast and ban will be executed in 3 seconds.");

                    // Update last use time
                    if (sender instanceof Player) {
                        lastAutoBanTime.put(((Player) sender).getUniqueId(), currentTime);
                    } else {
                        lastConsoleAutoBanTime = currentTime;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Player " + playerName + " not found or already banned.");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /autoban <Player>");
                return false;
            }
        }
        return false;
    }
}