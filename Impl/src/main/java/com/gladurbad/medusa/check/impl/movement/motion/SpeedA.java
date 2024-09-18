package com.gladurbad.medusa.check.impl.movement.motion;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

@CheckInfo(name = "Speed (A)", description = "Backup check to avoid being bypassed by damage, etc.")
public final class SpeedA extends Check {

    public SpeedA(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if(packet.isPosition() || packet.isPosLook()) {
            final double speed = data.getPositionProcessor().getDeltaXZ();
            debug("Speed " + speed);
            if(speed > 0.65 && !data.getPlayer().hasPotionEffect(PotionEffectType.SPEED) && !isExempt(ExemptType.FLYING, ExemptType.VELOCITY, ExemptType.TELEPORT, ExemptType.SLIME, ExemptType.PISTON)) {
                fail("Goofy speed " + speed);
            }
        }
    }
}