package com.gladurbad.medusa.exempt.type;

import com.gladurbad.medusa.QuantumTimeAC;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.util.PlayerUtil;
import com.gladurbad.medusa.util.ServerUtil;
import com.gladurbad.medusa.util.VersionUtil;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import lombok.Getter;

import java.util.function.Function;

@Getter
public enum ExemptType {

    CHUNK(data -> !data.getPlayer().getWorld().isChunkLoaded(data.getPlayer().getLocation().getBlockX() << 4,
            data.getPlayer().getLocation().getBlockZ() << 4)),

    TPS(data -> ServerUtil.getTPS() < 18.5D),

    HIGHPING(data -> PlayerUtil.getPing(data.getPlayer()) > 400),

    TELEPORT(data -> data.getPositionProcessor().isTeleporting() || System.currentTimeMillis() - data.getJoinTime() < 2000L),

    ANYVELOCITY(data -> data.getVelocityProcessor().isTakingVelocity()),

    VELOCITYEXC_FALL(data -> data.getVelocityProcessor().isTakingVelocity() && !data.getPlayer().getLastDamageCause().getCause().name().equals("FALL")),

    PVPVELOCITY(data -> data.getVelocityProcessor().isTakingVelocity() && data.getPlayer().getLastDamageCause().getCause().name().equals("ENTITY_ATTACK") || data.getPlayer().getLastDamageCause().getCause().name().equals("PROJECTILE")|| data.getPlayer().getLastDamageCause().getCause().name().equals("FIRE_TICK") || data.getPlayer().getLastDamageCause().getCause().name().equals("POISON") || data.getPlayer().getLastDamageCause().getCause().name().equals("WITHER") || data.getPlayer().getLastDamageCause().getCause().name().equals("THORNS")),

    JOINED(data -> System.currentTimeMillis() - data.getJoinTime() < 5000L),

    TRAPDOOR(data -> data.getPositionProcessor().isNearTrapdoor()),

    STEPPED(data -> data.getPositionProcessor().isOnGround() && data.getPositionProcessor().getDeltaY() > 0),

    CINEMATIC(data -> data.getRotationProcessor().isCinematic()),

    SLIME(data -> data.getPositionProcessor().getSinceSlimeTicks() < 30),

    ICE(data -> data.getPositionProcessor().getSinceIceTicks() < 40),

    SLAB(data -> data.getPositionProcessor().isNearSlab()),

    STAIRS(data -> data.getPositionProcessor().isNearStairs()),

    WEB(data -> data.getPositionProcessor().isInWeb()),

    CLIMBABLE(data -> data.getPositionProcessor().isOnClimbable()),

    DIGGING(data -> QuantumTimeAC.INSTANCE.getTickManager().getTicks() - data.getActionProcessor().getLastDiggingTick() < 10),

    BLOCK_BREAK(data -> QuantumTimeAC.INSTANCE.getTickManager().getTicks() - data.getActionProcessor().getLastBreakTick() < 10),

    PLACING(data -> QuantumTimeAC.INSTANCE.getTickManager().getTicks() - data.getActionProcessor().getLastPlaceTick() < 10),

    NEAR_VEHICLE(data -> data.getPositionProcessor().isNearVehicle()),

    INSIDE_VEHICLE(data -> data.getPositionProcessor().getSinceVehicleTicks() < 20),

    LIQUID(data -> data.getPositionProcessor().isInLiquid()),

    UNDER_BLOCK(data -> data.getPositionProcessor().isBlockNearHead()),

    PISTON(data -> data.getPositionProcessor().getSinceNearPistonTicks() < 50),

    VOID(data -> data.getPlayer().getLocation().getY() < 4),

    COMBAT(data -> data.getCombatProcessor().getHitTicks() < 5),

    FLYING(data -> data.getPositionProcessor().getSinceFlyingTicks() < 40),

    AUTO_CLICKER(data -> data.getExemptProcessor().isExempt(ExemptType.PLACING, ExemptType.DIGGING, ExemptType.BLOCK_BREAK)),

    DEPTH_STRIDER(data -> PlayerUtil.getDepthStriderLevel(data.getPlayer()) > 0),

    RIPTIDE(data -> VersionUtil.isRiptiding(data.getPlayer()) || 
        data.getPlayer().getInventory().getItemInHand().getType().toString().contains("TRIDENT")),

    SWIMMING(data -> VersionUtil.isSwimming(data.getPlayer())),

    CRAWLING(data -> VersionUtil.hasPose(data.getPlayer(), "SWIMMING") && !VersionUtil.isSwimming(data.getPlayer())),

    ELYTRA(data -> VersionUtil.isGliding(data.getPlayer())),

    BOAT(data -> data.getPlayer().getVehicle() != null && data.getPlayer().getVehicle().getType().toString().contains("BOAT")),

    STRIDER(data -> {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_16)) return false;
        return data.getPlayer().getVehicle() != null && data.getPlayer().getVehicle().getType().toString().equals("STRIDER");
    }),

    BUBBLE_COLUMN(data -> {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_13)) return false;
        return data.getPlayer().getLocation().getBlock().getType().toString().contains("BUBBLE_COLUMN");
    }),

    POWDER_SNOW(data -> {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_17)) return false;
        return data.getPlayer().getLocation().getBlock().getType().toString().equals("POWDER_SNOW");
    }),

    SCAFFOLDING(data -> {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_14)) return false;
        return data.getPlayer().getLocation().getBlock().getType().toString().equals("SCAFFOLDING");
    }),

    HONEY_BLOCK(data -> {
        if (ServerUtil.getServerVersion().isLowerThan(ServerVersion.v_1_15)) return false;
        return data.getPlayer().getLocation().getBlock().getType().toString().equals("HONEY_BLOCK");
    }),

    LEVITATION(data -> VersionUtil.hasLevitation(data.getPlayer())),

    SLOW_FALLING(data -> VersionUtil.hasSlowFalling(data.getPlayer()));

    private final Function<PlayerData, Boolean> exception;

    ExemptType(final Function<PlayerData, Boolean> exception) {
        this.exception = exception;
    }
}
