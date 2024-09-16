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
    double airTicks;
    boolean safetoflag;
    int pistonPushTicks = 0;
    int knockbackTicks = 0;
    int iceTicksCounter = 0;

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        Bukkit.broadcastMessage(String.valueOf(knockbackTicks));
        knockbackTicks = 10; // Beállítjuk a knockback időtartamát (1 másodperc)
    }

    @Override
    public void handle(final Packet packet) {
        final double deltaXZ = Math.abs(data.getPositionProcessor().getDeltaX());
        final double lastdeltadeltaXZ = Math.abs(data.getPositionProcessor().getLastDeltaXZ());
        final double lastDeltaY = data.getPositionProcessor().getLastDeltaY();
        final double Ymotion = data.getPlayer().getVelocity().getY();
        final double DeltaY = data.getPositionProcessor().getDeltaY();
        final double speed = data.getPositionProcessor().getDeltaXZ();
        final double bps = speed * 2;

        //SpeedA
        double speedLimit = 0.32;
        double airSpeedLimit = 0.342;

        // Ellenőrizzük, hogy a játékos jégen vagy slime blockon áll-e, vagy nemrég volt-e jégen
        boolean nearIce = data.getExemptProcessor().isExempt(ExemptType.ICE);
        boolean onSlime = data.getExemptProcessor().isExempt(ExemptType.SLIME);

        if (nearIce) {
            iceTicksCounter = 10; // Beállítjuk az ice ticks számlálót (0.5 másodperc)
        } else if (iceTicksCounter > 0) {
            iceTicksCounter--;
        }

        if (nearIce || iceTicksCounter > 0) {
            speedLimit = 0.6; // Növeljük a sebességkorlátot jégen vagy jég közelében
            airSpeedLimit = 0.6; // Növeljük a levegőben való sebességkorlátot is jég felett vagy közelében
        } else if (onSlime) {
            speedLimit = 0.5; // Növeljük a sebességkorlátot slime blockon
            airSpeedLimit = 0.5; // Növeljük a levegőben való sebességkorlátot is slime block felett
        }

        // Ellenőrizzük, hogy a játékost nemrég meglökte-e egy piston vagy megütötte-e egy másik játékos
        if (pistonPushTicks > 0 || knockbackTicks > 0) {
            speedLimit += 0.3; // Növeljük a sebességkorlátot piston lökés vagy ütés után
            airSpeedLimit += 0.3; // Növeljük a levegőben való sebességkorlátot is
            pistonPushTicks = Math.max(0, pistonPushTicks - 1);
            knockbackTicks = Math.max(0, knockbackTicks - 1);
        }

        // Ellenőrizzük, hogy a játékost most lökte-e meg egy piston
        if (data.getPlayer().getVelocity().length() > 0.4) {
            pistonPushTicks = 20; // Beállítjuk a piston lökés időtartamát (1 másodperc)
        }

        if(speed > speedLimit && data.getPositionProcessor().isOnGround() && data.getPositionProcessor().getAirTicks() > 3 && !data.getPlayer().hasPotionEffect(PotionEffectType.SPEED) && !isExempt() && !isExempt(ExemptType.VELOCITY, ExemptType.FLYING)) {
            fail("speed " + speed + " (limit: " + speedLimit + ")");
        } else if(speed > airSpeedLimit && !data.getPlayer().hasPotionEffect(PotionEffectType.SPEED) && data.getPositionProcessor().getAirTicks() > 5 && !isExempt(ExemptType.FLYING) && !isExempt(ExemptType.VELOCITY) && !isExempt(ExemptType.FLYING)) {
            fail("airspeed " + speed + " (limit: " + airSpeedLimit + ")");
        }
    }
}