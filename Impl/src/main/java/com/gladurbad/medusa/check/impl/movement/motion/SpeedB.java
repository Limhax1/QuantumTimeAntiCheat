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

@CheckInfo(name = "Speed (B)", description = "Checks for autistic flight modules.")
public final class SpeedB extends Check {

    public SpeedB(final PlayerData data) {
        super(data);
    }
    int knockbackTicks = 0;
    double BUFFER;
    double MAX_BUFFER = 7;

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        Bukkit.broadcastMessage(String.valueOf(knockbackTicks));
        knockbackTicks = 10; // Beállítjuk a knockback időtartamát (1 másodperc)
    }

    @Override
    public void handle(final Packet packet) {
        if(packet.isPosition() || packet.isPosLook()) {

            final double deltaXZ = Math.abs(data.getPositionProcessor().getDeltaX());
            final double lastdeltadeltaXZ = Math.abs(data.getPositionProcessor().getLastDeltaXZ());
            final double lastDeltaY = data.getPositionProcessor().getLastDeltaY();
            final double Ymotion = data.getPlayer().getVelocity().getY();
            final double DeltaY = data.getPositionProcessor().getDeltaY();
            final double speed = data.getPositionProcessor().getDeltaXZ();
            final double bps = speed * 2;

            //SpeedA
            double speedLimit = 0.32;
            double airSpeedLimit = 0.3546;

            boolean isexempt = isExempt(ExemptType.VELOCITY, ExemptType.FLYING, ExemptType.TELEPORT, ExemptType.STAIRS, ExemptType.SLIME, ExemptType.ICE);

            debug("" + speed + " " + speedLimit);

            if(!isexempt) {
                if (speed > speedLimit && data.getPositionProcessor().isOnGround() && !data.getPlayer().hasPotionEffect(PotionEffectType.SPEED)) {
                    fail("speed " + speed + " (limit: " + speedLimit + ")");
                } else if (speed > airSpeedLimit && !data.getPlayer().hasPotionEffect(PotionEffectType.SPEED) && data.getPositionProcessor().isInAir()) {
                    fail("airspeed " + speed + " (limit: " + airSpeedLimit + ")");
                }
            }
        }
    }
}