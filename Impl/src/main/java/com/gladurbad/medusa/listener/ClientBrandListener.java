package com.gladurbad.medusa.listener;

import com.gladurbad.medusa.QuantumTimeAC;
import com.gladurbad.medusa.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

public final class ClientBrandListener implements PluginMessageListener, Listener {

    private static final String OLD_CHANNEL = "MC|Brand";
    private static final String NEW_CHANNEL = "minecraft:brand";

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] msg) {
        if (channel.equals(OLD_CHANNEL) || channel.equals(NEW_CHANNEL)) {
            try {
                final PlayerData data = QuantumTimeAC.INSTANCE.getPlayerDataManager().getPlayerData(player);
                if (data == null) return;
                data.setClientBrand(new String(msg, "UTF-8").substring(1));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        addChannel(player, OLD_CHANNEL);
        addChannel(player, NEW_CHANNEL);
    }

    private void addChannel(final Player player, final String channel) {
        try {
            player.getClass().getMethod("addChannel", String.class).invoke(player, channel);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            // Ignore exceptions, as they might occur in different versions
        }
    }
}
