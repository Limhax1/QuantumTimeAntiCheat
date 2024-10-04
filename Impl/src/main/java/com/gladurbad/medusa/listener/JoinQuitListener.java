package com.gladurbad.medusa.listener;

import com.gladurbad.medusa.QuantumTimeAC;
import com.gladurbad.medusa.util.anticheat.AlertUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinQuitListener implements Listener {

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        QuantumTimeAC.INSTANCE.getPlayerDataManager().add(event.getPlayer());

        if (event.getPlayer().hasPermission("qatc.alerts")) {
            AlertUtil.toggleAlerts(QuantumTimeAC.INSTANCE.getPlayerDataManager().getPlayerData(event.getPlayer()));
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        QuantumTimeAC.INSTANCE.getPlayerDataManager().remove(event.getPlayer());
    }
}
