package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.entity.Entity;

@CheckInfo(name = "Aim (P)", description = "Checks for invalid yaw changes", complextype = "Invalid Yaw")
public class AimP extends Check {

    public AimP(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isRotation()) {
            Entity target = data.getCombatProcessor().getTarget();
            if (target == null) return;
            if(data.getPlayer().getLastDamageCause().getCause() == null) return;
            debug(data.getPlayer().getLastDamageCause().getCause().name());
        }
    }
}
/*
    data.getPlayer().getLastDamageCause().getCause().name().equals("ENTITY_ATTACK")
 */