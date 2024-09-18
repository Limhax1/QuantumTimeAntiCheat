package com.gladurbad.medusa.check.impl.movement.jesus;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

@CheckInfo(name = "Jesus (A)", description = "Checks for Jesus hacks", experimental = true)
public class JesusA extends Check {

    public JesusA(final PlayerData data) {
        super(data);
    }

    double BUFFER;
    double MAX_BUFFER = 2;

    @Override
    public void handle(Packet packet) {
        if(packet.isPosition() || packet.isPosLook()) {

            boolean posAir = data.getPlayer().getLocation().getBlock().isEmpty();

            debug("DY: " + data.getPositionProcessor().getDeltaY() + " NC: " + nearCoast(data.getPlayer().getLocation()));
            if(data.getPlayer().getLocation().add(0, -1, 0).getBlock().isLiquid() && posAir && !nearCoast(data.getPlayer().getLocation())) {
                if(data.getPositionProcessor().getDeltaY() == 0 || data.getPositionProcessor().getDeltaY() == 0.33319999363422426) {
                    if(BUFFER++ > MAX_BUFFER) {
                        fail("Hovering over water: " + data.getPositionProcessor().getDeltaY());
                        BUFFER = 0;
                    } else {
                        BUFFER = Math.max(0, BUFFER - 0.50);
                    }
                }
            }

            if(data.getPlayer().getLocation().getBlock().isLiquid()) {
                debug("DY: " + data.getPositionProcessor().getDeltaY() + " NC: " + nearCoast(data.getPlayer().getLocation()));
                if(data.getPositionProcessor().getDeltaY() == -0.25) {
                    fail("Invalid motion: " + data.getPositionProcessor().getDeltaY());
                }
            }
        }
    }

    private boolean nearCoast(Location location) {
        for (double x = -0.5; x <= 0.5; x++) {
            for (double y = -1.5; y <= 1.5; y++) {
                for (double z = -0.5; z <= 0.5; z++) {
                    Block block = location.getBlock().getRelative((int) x, (int) y, (int) z);
                    Material type = block.getType();
                    if (!block.isLiquid() && !block.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
