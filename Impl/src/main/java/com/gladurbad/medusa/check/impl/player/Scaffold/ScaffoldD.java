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
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Scaffold (D)", description = "Fejlett sugárkövetéses ellenőrzés lehetetlen blokk elhelyezések észlelésére.")
public class ScaffoldD extends Check {

    private static final double MAX_REACH = 5.5;
    private static final int BUFFER_LIMIT = 5;
    private static final double POINT_SCALE = 0.01;

    private int buffer = 0;
    private Block lastPlacedBlock = null;

    public ScaffoldD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isBlockPlace()) {
            Player player = data.getPlayer();
            Location eyeLocation = player.getEyeLocation();
            Vector direction = eyeLocation.getDirection();
            
            RayTrace rayTrace = new RayTrace(player, eyeLocation, direction, MAX_REACH, POINT_SCALE);
            RayTraceResult result = rayTrace.trace();

            Block placedBlock = getTargetBlock(player, MAX_REACH);

            if (placedBlock != null && placedBlock.getType() != Material.AIR) {
                Location blockLocation = placedBlock.getLocation();
                
                boolean validPlacement = checkValidPlacement(result, placedBlock);

                if (!validPlacement && !isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE)) {
                    buffer++;

                    if (buffer > BUFFER_LIMIT) {
                        Location hitLoc = result.getHitLocation();
                        fail("Lehetetlen blokk elhelyezés.");
                        buffer = BUFFER_LIMIT / 2;
                    }
                } else {
                    buffer = Math.max(0, buffer - 1);
                }

                Location hitLoc = result.getHitLocation();
                debug("Blokk: %.2f, %.2f, %.2f, Sugárkövetés: %.2f, %.2f, %.2f, Érvényes: %b, Buffer: %d", 
                      blockLocation.getX(), blockLocation.getY(), blockLocation.getZ(),
                      hitLoc != null ? hitLoc.getX() : 0, 
                      hitLoc != null ? hitLoc.getY() : 0, 
                      hitLoc != null ? hitLoc.getZ() : 0,
                      validPlacement, 
                      buffer);
            }

            lastPlacedBlock = placedBlock;
        }
    }

    private boolean checkValidPlacement(RayTraceResult result, Block placedBlock) {
        if (result.getHitType() == RayTraceResult.HitType.BLOCK) {
            Location hitLocation = result.getHitLocation();
            Block hitBlock = hitLocation.getBlock();
            
            return hitBlock.equals(placedBlock) || isNeighbor(hitBlock, placedBlock);
        }
        return false;
    }

    private boolean isNeighbor(Block block1, Block block2) {
        for (BlockFace face : BlockFace.values()) {
            if (block1.getRelative(face).equals(block2)) {
                return true;
            }
        }
        return false;
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