package com.gladurbad.medusa.util.type;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import lombok.Getter;
import lombok.Setter;

@Getter
public class LocationVector {
    @Setter
    private Long timestamp;
    private final double x,y,z;

    public LocationVector(Vector3d vector) {
        this.x = vector.getX();
        this.y = vector.getY();
        this.z = vector.getZ();
    }

    public LocationVector(Double x, Double y, Double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public LocationVector add(LocationVector other) {
        return new LocationVector(new Vector3d(this.x + other.x, this.y + other.y, this.z + other.z));
    }
}
