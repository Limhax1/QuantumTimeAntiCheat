package com.gladurbad.medusa;

import io.github.retrooper.packetevents.PacketEvents;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class MedusaPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        PacketEvents.create(this).getSettings()
                .checkForUpdates(false);

        PacketEvents.get().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.get().init();
        Medusa.INSTANCE.start(this);
    }

    @Override
    public void onDisable() {
        PacketEvents.get().terminate();
        Medusa.INSTANCE.stop(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("custombroadcast")) {
            if (args.length > 1) {
                String playerName = args[0];
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if(!Bukkit.getPlayer(playerName).isBanned()) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tempban " + playerName + " 1s Unfair Advantage");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Használat: /custombroadcast <játékosnév> <üzenet>");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("acban")) {
            if (args.length >= 1) {
                String playerName = args[0];
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Player targetPlayer = Bukkit.getPlayer(playerName);
                String duration = "1s";
                if (targetPlayer.isOnline() && !targetPlayer.isBanned()) {
                    String banCommand = "tempban " + playerName + " " +  duration + " Unfair Advantage";
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), banCommand);
                    sender.sendMessage(ChatColor.GREEN + "Player " + playerName + " has successfully been force banned.");
                } else if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player " + playerName + " wasnt found");
                } else {
                    sender.sendMessage(ChatColor.RED + "Player " + playerName + " is already banned");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Használat: /acban <játékosnév> <indok>");
                return false;
            }
        }
        return false;
    }
}
