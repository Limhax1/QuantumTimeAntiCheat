package com.gladurbad.medusa.check.impl.combat.hitbox;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Hitbox (A)", description = "Checks for invalid attack angles.")
public class HitboxA extends Check {

    private static final double MAX_ANGLE = Math.toRadians(30); // 90 fokos maximális szög
    private static final int BUFFER_LIMIT = 5;

    private int buffer;

    public HitboxA(PlayerData data) {
        super(data);
        this.buffer = 0;
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isUseEntity()) {
            WrappedPacketInUseEntity wrappedPacket = new WrappedPacketInUseEntity(packet.getRawPacket());

            if (wrappedPacket.getAction() != WrappedPacketInUseEntity.EntityUseAction.ATTACK) return;

            Player attacker = data.getPlayer();
            Entity target = wrappedPacket.getEntity();

            if (!(target instanceof Player)) return;

            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE);

            if (!exempt) {
                double angle = calculateAngle(attacker, (Player) target);

                debug("angle=" + String.format("%.2f", Math.toDegrees(angle)) +
                      " max=" + String.format("%.2f", Math.toDegrees(MAX_ANGLE)) +
                      " buffer=" + buffer);

                if (angle > MAX_ANGLE) {
                    if (++buffer > BUFFER_LIMIT) {
                        fail(String.format("Invalid attack angle. Angle: %.2f, Max: %.2f, Buffer: %d",
                       Math.toDegrees(angle), Math.toDegrees(MAX_ANGLE), buffer));
                        buffer = 0;
                    }
                } else {
                    buffer = Math.max(buffer - 1, 0);
                }
            }
        }
    }

    private double calculateAngle(Player attacker, Player victim) {
        Vector attackerDirection = attacker.getLocation().getDirection();
        Vector victimDirection = victim.getLocation().subtract(attacker.getLocation()).toVector().normalize();
        return Math.acos(attackerDirection.dot(victimDirection));
    }
}