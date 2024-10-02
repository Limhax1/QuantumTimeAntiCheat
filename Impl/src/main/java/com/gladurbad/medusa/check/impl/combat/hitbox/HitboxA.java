package com.gladurbad.medusa.check.impl.combat.hitbox;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Hitbox (A)", description = "Checks for invalid attack angles and hitbox expansion.", complextype = "Hitbox")
public class HitboxA extends Check {

    private static final double MAX_REACH = 3.5; // Maximális elérési távolság
    private static final double HITBOX_WIDTH = 1.0; // Megnövelt játékos szélesség
    private static final double HITBOX_HEIGHT = 2.2; // Megnövelt játékos magasság
    private static final double MOVEMENT_COMPENSATION = 0.3; // Mozgás kompenzáció
    private static final int BUFFER_LIMIT = 7; // Megnövelt buffer limit

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

            Player victim = (Player) target;

            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE, ExemptType.JOINED);
            
            if (!exempt) {
                Location eyeLocation = attacker.getEyeLocation();
                Vector direction = eyeLocation.getDirection();

                // Kompenzáljuk a mozgást
                Vector attackerVelocity = attacker.getVelocity();
                Vector victimVelocity = victim.getVelocity();
                Vector relativeVelocity = victimVelocity.subtract(attackerVelocity);

                Vector hitboxIntersection = calculateHitboxIntersection(eyeLocation, direction, victim.getLocation(), relativeVelocity);
                
                if (hitboxIntersection == null) {
                    if (++buffer > BUFFER_LIMIT) {
                        fail("Hit outside of hitbox");
                        buffer = BUFFER_LIMIT / 2;
                    }
                } else {
                    double distance = eyeLocation.distance(hitboxIntersection.toLocation(attacker.getWorld()));
                    
                    if (distance > MAX_REACH) {
                        if (++buffer > BUFFER_LIMIT) {
                            fail(String.format("Distance: %.2f", distance));
                            buffer = BUFFER_LIMIT / 2;
                        }
                    } else {
                        buffer = Math.max(0, buffer - 1);
                    }
                }

                debug(String.format("Distance: %.2f, Buffer: %d, Intersection: %s", 
                        eyeLocation.distance(victim.getLocation()),
                        buffer,
                        hitboxIntersection != null ? "Hit" : "Miss"));
            }
        }
    }

    private Vector calculateHitboxIntersection(Location eyeLocation, Vector direction, Location victimLocation, Vector relativeVelocity) {
        Vector playerMin = victimLocation.toVector().subtract(new Vector(HITBOX_WIDTH / 2, 0, HITBOX_WIDTH / 2));
        Vector playerMax = playerMin.clone().add(new Vector(HITBOX_WIDTH, HITBOX_HEIGHT, HITBOX_WIDTH));

        // Kompenzáljuk a mozgást
        playerMin.add(relativeVelocity.multiply(MOVEMENT_COMPENSATION));
        playerMax.add(relativeVelocity.multiply(MOVEMENT_COMPENSATION));

        Vector origin = eyeLocation.toVector();

        double tMin = 0.0;
        double tMax = Double.MAX_VALUE;

        for (int i = 0; i < 3; i++) {
            double originComp = (i == 0) ? origin.getX() : ((i == 1) ? origin.getY() : origin.getZ());
            double directionComp = (i == 0) ? direction.getX() : ((i == 1) ? direction.getY() : direction.getZ());
            double minComp = (i == 0) ? playerMin.getX() : ((i == 1) ? playerMin.getY() : playerMin.getZ());
            double maxComp = (i == 0) ? playerMax.getX() : ((i == 1) ? playerMax.getY() : playerMax.getZ());

            if (Math.abs(directionComp) < 1e-8) {
                if (originComp < minComp || originComp > maxComp) {
                    return null;
                }
            } else {
                double invD = 1.0 / directionComp;
                double t1 = (minComp - originComp) * invD;
                double t2 = (maxComp - originComp) * invD;

                if (t1 > t2) {
                    double temp = t1;
                    t1 = t2;
                    t2 = temp;
                }

                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);

                if (tMin > tMax) {
                    return null;
                }
            }
        }

        Vector intersection = origin.clone().add(direction.multiply(tMin));

        if (intersection.getX() < playerMin.getX() || intersection.getX() > playerMax.getX() ||
                intersection.getY() < playerMin.getY() || intersection.getY() > playerMax.getY() ||
                intersection.getZ() < playerMin.getZ() || intersection.getZ() > playerMax.getZ()) {
            return null;
        }

        return intersection;
    }
}