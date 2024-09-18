package com.gladurbad.medusa.util.raytrace;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import com.gladurbad.medusa.util.type.BoundingBox;

import java.util.List;

public class RayTrace {
    private final Vector origin;
    private final Vector direction;
    private final double maxDistance;
    private final double pointScale;
    private final World world;

    private boolean render = false;

    public RayTrace(Location start, Vector direction, double maxDistance, double pointScale) {
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
                BoundingBox playerBox = new BoundingBox(player);
                Vector min = new Vector(playerBox.getMinX(), playerBox.getMinY(), playerBox.getMinZ());
                Vector max = new Vector(playerBox.getMaxX(), playerBox.getMaxY(), playerBox.getMaxZ());

                double tMin = 0;
                double tMax = maxDistance;

                for (int i = 0; i < 3; i++) {
                    double origin_i = origin(i).length();
                    double direction_i = direction(i).length();

                    if (Math.abs(direction_i) < 1e-8) {
                        if (origin_i < min.getX() || origin_i > max.getX()) {
                            continue;
                        }
                    } else {
                        double t1 = (min.getX() - origin_i) / direction_i;
                        double t2 = (max.getX() - origin_i) / direction_i;

                        if (t1 > t2) {
                            double temp = t1;
                            t1 = t2;
                            t2 = temp;
                        }

                        tMin = Math.max(tMin, t1);
                        tMax = Math.min(tMax, t2);

                        if (tMin > tMax) {
                            continue;
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
