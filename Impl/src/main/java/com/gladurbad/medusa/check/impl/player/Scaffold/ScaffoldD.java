package com.gladurbad.medusa.check.impl.player.Scaffold;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.raytrace.RayTrace;
import com.gladurbad.medusa.util.raytrace.RayTraceResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Scaffold (D)", description = "Detects impossible block placements using ray tracing.")
public class ScaffoldD extends Check {

    private static final double MAX_REACH = 5.0;
    private static final double BUFFER_LIMIT = 5.0;
    private static final double POINT_SCALE = 0.1;

    private double buffer = 0.0;
    private Block lastPlacedBlock = null;

    public ScaffoldD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        Player player = data.getPlayer();

        if (packet.isBlockPlace()) {
            Location eyeLocation = player.getEyeLocation();
            Vector direction = eyeLocation.getDirection();
            
            RayTrace rayTrace = new RayTrace(player, eyeLocation, direction, MAX_REACH, POINT_SCALE);
            RayTraceResult result = rayTrace.trace();

            if (lastPlacedBlock != null && lastPlacedBlock.getType() != Material.AIR) {
                Location blockLocation = lastPlacedBlock.getLocation();
                
                boolean validPlacement = false;

                if (result.getHitType() == RayTraceResult.HitType.BLOCK) {
                    Location hitLocation = result.getHitLocation();
                    if (hitLocation.getBlock().equals(lastPlacedBlock)) {
                        validPlacement = true;
                    }
                } else {
                    validPlacement = false;
                }

                if (!validPlacement && !isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE)) {
                    buffer += 1.0;

                    if (buffer > BUFFER_LIMIT) {
                        fail(String.format("Impossible block placement. Block: %s, RayTrace: %s", 
                                           blockLocation.toVector(), result.getHitLocation() != null ? result.getHitLocation().toVector() : "No hit"));
                        buffer = BUFFER_LIMIT / 2;
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.5);
                }

                debug(String.format("Block: %s, RayTrace: %s, Valid: %b, Buffer: %.2f", 
                                    blockLocation.toVector(), 
                                    result.getHitLocation() != null ? result.getHitLocation().toVector() : "No hit", 
                                    validPlacement, 
                                    buffer));
            }

            // Store the placed block for the next check
            lastPlacedBlock = getTargetBlock(player, MAX_REACH);
        }
    }

    private Block getTargetBlock(Player player, double maxDistance) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        RayTrace rayTrace = new RayTrace(player, eyeLocation, direction, maxDistance, POINT_SCALE);
        RayTraceResult result = rayTrace.trace();

        if (result.getHitType() == RayTraceResult.HitType.BLOCK) {
            return result.getHitLocation().getBlock();
        }
        return null;
    }
}