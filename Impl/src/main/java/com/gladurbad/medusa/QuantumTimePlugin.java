package com.gladurbad.medusa;

import io.github.retrooper.packetevents.PacketEvents;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuantumTimePlugin extends JavaPlugin {

    private final Map<UUID, Long> lastCustomBroadcastTime = new HashMap<>();
    private long lastConsoleBroadcastTime = 0L;

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
        if (command.getName().equalsIgnoreCase("custombroadcast") && sender.hasPermission("qtac.custombroadcast")) {
            long currentTime = System.currentTimeMillis();
            long lastUseTime;

            if (sender instanceof Player) {
                Player player = (Player) sender;
                lastUseTime = lastCustomBroadcastTime.getOrDefault(player.getUniqueId(), 0L);
            } else {
                lastUseTime = lastConsoleBroadcastTime;
            }

            if (currentTime - lastUseTime < 1000) {
                sender.sendMessage(ChatColor.RED + "Please wait 1 second before using this command again.");
                return true;
            }

            if (sender instanceof Player) {
                lastCustomBroadcastTime.put(((Player) sender).getUniqueId(), currentTime);
            } else {
                lastConsoleBroadcastTime = currentTime;
            }

            if (args.length > 1) {
                String duration = "3s";
                String playerName = args[0];
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Player targetPlayer = Bukkit.getPlayer(playerName);

                if (targetPlayer != null && !targetPlayer.isBanned()) {
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tempban " + playerName + " " + duration + " Unfair Advantage");
                    }, 20L); // 20 tick = 1 másodperc
                    sender.sendMessage(ChatColor.GREEN + "Broadcast and ban will be executed in 1 second.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Player " + playerName + " not found or already banned.");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /custombroadcast <Player> <Message>");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("forceban") && sender.hasPermission("qtac.forceban")) {
            if (args.length >= 1) {
                String playerName = args[0];
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Player targetPlayer = Bukkit.getPlayer(playerName);
                String duration = "1s";
                if (targetPlayer.isOnline() && !targetPlayer.isBanned()) {
                    String banCommand = "tempban " + playerName + " " + duration + " Unfair Advantage";
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), banCommand);
                    sender.sendMessage(ChatColor.GREEN + "Player " + playerName + " has successfully been force banned.");
                } else if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player " + playerName + " wasn't found");
                } else {
                    sender.sendMessage(ChatColor.RED + "Player " + playerName + " is already banned");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /forceban <Player> <Reason>");
                return false;
            }
        }
        return false;
    }
}
