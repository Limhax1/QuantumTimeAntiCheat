package com.gladurbad.medusa.util.raytrace;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RayTraceResult {
    public enum HitType {
        NONE,
        BLOCK,
        ENTITY
    }

    private final HitType hitType;
    private final Location hitLocation;
    private final double distance;
    private final Player hitEntity;

    public RayTraceResult(HitType hitType, Location hitLocation, double distance, Player hitEntity) {
        this.hitType = hitType;
        this.hitLocation = hitLocation;
        this.distance = distance;
        this.hitEntity = hitEntity;
    }

    public HitType getHitType() {
        return hitType;
    }

    public Location getHitLocation() {
        return hitLocation;
    }

    public double getDistance() {
        return distance;
    }

    public Player getHitEntity() {
        return hitEntity;
    }

    public boolean hasHit() {
        return hitType != HitType.NONE;
    }
}
