package com.gladurbad.medusa.util.raytrace;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import com.gladurbad.medusa.util.type.BoundingBox;

import java.util.List;

public class RayTrace {

    private final Player player;
    private final Vector origin;
    private final Vector direction;
    private final double maxDistance;
    private final double pointScale;
    private final World world;

    private boolean render = false;

    public RayTrace(Player player, Location start, Vector direction, double maxDistance, double pointScale) {
        this.player = player;
        this.world = start.getWorld();
        this.origin = start.toVector();
        this.direction = direction.normalize();
        this.maxDistance = maxDistance;
        this.pointScale = pointScale;
    }

    public RayTraceResult trace() {
        List<Player> players = world.getPlayers();
        Vector currentPos = origin.clone();
        double distanceTraveled = 0;

        while (distanceTraveled <= maxDistance) {
            spawnParticles(Effect.FLAME, currentPos.toLocation(world), 1);

            Location blockLoc = currentPos.toLocation(world);
            if (blockLoc.getBlock().getType().isSolid()) {
                return new RayTraceResult(RayTraceResult.HitType.BLOCK, currentPos.toLocation(world), distanceTraveled, null);
            }

            for (Player player : players) {
                if (player.equals(this.player)) continue;
                BoundingBox playerBox = new BoundingBox(player);
                double tMin = 0;
                double tMax = maxDistance;

                for (int i = 0; i < 3; i++) {
                    double axisOrigin = i == 0 ? origin.getX() : (i == 1 ? origin.getY() : origin.getZ());
                    double axisDirection = i == 0 ? direction.getX() : (i == 1 ? direction.getY() : direction.getZ());
                    double axisMin = i == 0 ? playerBox.getMinX() : (i == 1 ? playerBox.getMinY() : playerBox.getMinZ());
                    double axisMax = i == 0 ? playerBox.getMaxX() : (i == 1 ? playerBox.getMaxY() : playerBox.getMaxZ());

                    if (Math.abs(axisDirection) < 1e-8) {
                        if (axisOrigin < axisMin || axisOrigin > axisMax) {
                            tMin = maxDistance + 1;
                            break;
                        }
                    } else {
                        double t1 = (axisMin - axisOrigin) / axisDirection;
                        double t2 = (axisMax - axisOrigin) / axisDirection;

                        if (t1 > t2) {
                            double temp = t1;
                            t1 = t2;
                            t2 = temp;
                        }

                        tMin = Math.max(tMin, t1);
                        tMax = Math.min(tMax, t2);

                        if (tMin > tMax) {
                            tMin = maxDistance + 1;
                            break;
                        }
                    }
                }

                if (tMin <= tMax && tMin < maxDistance) {
                    Vector hitLocation = origin.clone().add(direction.clone().multiply(tMin));
                    return new RayTraceResult(RayTraceResult.HitType.ENTITY, hitLocation.toLocation(world), tMin, player);
                }
            }

            currentPos.add(direction.clone().multiply(pointScale));
            distanceTraveled += pointScale;
        }

        return new RayTraceResult(RayTraceResult.HitType.NONE, null, maxDistance, null);
    }

    public void spawnParticles(Effect particle, Location location, int count) {
        if (!render) return;
        
        //idk ill make it work some time later
    }

    public Vector origin(int i) {
        switch (i) {
            case 0: return new Vector(origin.getX(), 0, 0);
            case 1: return new Vector(0, origin.getY(), 0);
            case 2: return new Vector(0, 0, origin.getZ());
            default: return new Vector();
        }
    }

    public Vector direction(int i) {
        switch (i) {
            case 0: return new Vector(direction.getX(), 0, 0);
            case 1: return new Vector(0, direction.getY(), 0);
            case 2: return new Vector(0, 0, direction.getZ());
            default: return new Vector();
        }
    }
}
