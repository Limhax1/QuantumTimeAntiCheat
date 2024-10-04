package com.gladurbad.medusa.data.processor;

import com.gladurbad.medusa.util.JavaV;
import com.gladurbad.medusa.util.PlayerUtil;
import com.gladurbad.medusa.util.type.BoundingBox;
import com.gladurbad.medusa.util.type.LocationVector;
import com.gladurbad.medusa.util.type.Pair;
import com.google.common.cache.CacheBuilder;
import com.gladurbad.medusa.QuantumTimeAC;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityteleport.WrappedPacketOutEntityTeleport;
import io.github.retrooper.packetevents.packetwrappers.play.out.namedentityspawn.WrappedPacketOutNamedEntitySpawn;
import io.github.retrooper.packetevents.packetwrappers.play.out.position.WrappedPacketOutPosition;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Stairs;
import org.bukkit.material.Step;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;

@Getter
public final class PositionProcessor {
    private int sinceSpeedTicks;
    private final PlayerData data;

    private Map<Integer, Deque<LocationVector>> recentPlayerMoves = createCache(TimeUnit.HOURS.toMillis(1L), null);

    private Map<Integer, Pair<Boolean, Location>> recentPositions = createCache(null, TimeUnit.MINUTES.toMillis(10L));

    private final double divider = ServerVersion.getVersion().isNewerThan(ServerVersion.v_1_8_3) ? 4096.0 : 32.0;

    private double x, y, z,
            lastX, lastY, lastZ,
            deltaX, deltaY, deltaZ, deltaXZ,
            lastDeltaX, lastDeltaZ, lastDeltaY, lastDeltaXZ;

    private boolean flying, inVehicle, inLiquid, inAir, inWeb,
            blockNearHead, onClimbable, onSolidGround, nearVehicle, onSlime,
            onIce, nearPiston, nearTrapdoor, nearSlab, nearStairs, teleporting;

    private int airTicks, sinceVehicleTicks, sinceFlyingTicks,
            groundTicks, sinceSlimeTicks, solidGroundTicks,
            iceTicks, sinceIceTicks, blockNearHeadTicks, sinceBlockNearHeadTicks,
            sinceNearPistonTicks, tpBandaidFixTicks;

    private final ArrayDeque<Vector> teleports = new ArrayDeque<>();

    private BoundingBox boundingBox;

    private boolean onGround, lastOnGround, mathematicallyOnGround;

    private final List<Block> blocks = new ArrayList<>();

    private LinkedList<Location> recentGroundLocations = new LinkedList<>();
    private static final int MAX_GROUND_LOCATIONS = 200;
    private static final long SETBACK_DURATION = 50; // 50ms
    private long setbackTime = 0;

    public PositionProcessor(final PlayerData data) {
        this.sinceSpeedTicks = 100;
        this.data = data;
    }


    public void handleFlying(final WrappedPacketInFlying wrapper) {
        int tick = QuantumTimeAC.INSTANCE.getTickManager().getTicks();
        World playerWorld = data.getPlayer().getWorld();
        Vector3d postision = wrapper.getPosition();
        Location bukkitLocation = new Location(playerWorld, postision.getX(), postision.getY(), postision.getZ());

        Pair<Boolean, Location> pair = new Pair<Boolean,Location>( postision.getY() % 0.015625 == 0.0 && groundTicks > 2, bukkitLocation);
        recentPositions.put(tick, pair);
    }

    public void setback() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - setbackTime < SETBACK_DURATION) {
            return;
        }
        setbackTime = currentTime;

        Location playerLocation = data.getPlayer().getLocation();
        Location bestLocation = null;
        double shortestDistance = Double.MAX_VALUE;

        for (Location location : recentGroundLocations) {
            double distance = location.distanceSquared(playerLocation);
            if (distance < shortestDistance && isValidGroundLocation(location)) {
                shortestDistance = distance;
                bestLocation = location;
            }
        }

        if (bestLocation != null) {

            float yaw = data.getRotationProcessor().getYaw();
            float pitch = data.getRotationProcessor().getPitch();
            bestLocation.setYaw(yaw);
            bestLocation.setPitch(pitch);
            data.getPlayer().teleport(bestLocation);
        } else {
            float yaw = data.getRotationProcessor().getYaw();
            float pitch = data.getRotationProcessor().getPitch();
            Location loc = data.getPlayer().getLocation();
            loc.setYaw(yaw);
            loc.setPitch(pitch);
            data.getPlayer().teleport(loc);
        }
    }

    public void handle(final double x, final double y, final double z, final boolean onGround) {
        //FIX THIS TELEPORT SYSTEM.
        if (teleports.size() > 0) {
            tpBandaidFixTicks = 2;
            teleporting = true;
        }

        if (teleports.size() == 0) {
            if (--tpBandaidFixTicks < 0) {
                teleporting = false;
            }
        }

        lastX = this.x;
        lastY = this.y;
        lastZ = this.z;
        this.lastOnGround = this.onGround;

        this.x = x;
        this.y = y;
        this.z = z;
        this.onGround = onGround;

        handleCollisions();

        lastDeltaX = deltaX;
        lastDeltaY = deltaY;
        lastDeltaZ = deltaZ;
        lastDeltaXZ = deltaXZ;

        deltaX = this.x - lastX;
        deltaY = this.y - lastY;
        deltaZ = this.z - lastZ;
        deltaXZ = Math.hypot(deltaX, deltaZ);

        if (teleports.size() > 150) {
            teleports.remove(0);
        }

        for (Vector vector : teleports) {
            final double dx = Math.abs(x - vector.getX());
            final double dy = Math.abs(y - vector.getY());
            final double dz = Math.abs(z - vector.getZ());

            if (dx == 0 && dy == 0 && dz == 0) {
                teleports.remove(vector);
            }
        }

        mathematicallyOnGround = y % 0.015625 == 0.0;

        if (onGround && mathematicallyOnGround) {
            Location currentLocation = new Location(data.getPlayer().getWorld(), x, y, z);
            if (isValidGroundLocation(currentLocation)) {
                recentGroundLocations.addFirst(currentLocation);
                if (recentGroundLocations.size() > MAX_GROUND_LOCATIONS) {
                    recentGroundLocations.removeLast();
                }
            }
        }
    }

    public void handleTicks() {
        groundTicks = onGround && mathematicallyOnGround ? groundTicks + 1 : 0;
        blockNearHeadTicks = blockNearHead ? blockNearHeadTicks + 1 : 0;
        sinceNearPistonTicks = nearPiston ? 0 : sinceNearPistonTicks + 1;
        sinceBlockNearHeadTicks = blockNearHead ? 0 : sinceBlockNearHeadTicks + 1;
        airTicks = inAir ? airTicks + 1 : 0;
        inVehicle = data.getPlayer().isInsideVehicle();
        sinceVehicleTicks = inVehicle ? 0 : sinceVehicleTicks + 1;
        iceTicks = onIce ? iceTicks + 1 : 0;
        sinceIceTicks = onIce ? 0 : sinceIceTicks + 1;
        solidGroundTicks = onSolidGround ? solidGroundTicks + 1 : 0;
        flying = data.getPlayer().isFlying();
        sinceFlyingTicks = flying ? 0 : sinceFlyingTicks + 1;
        sinceSlimeTicks = onSlime ? 0 : sinceSlimeTicks + 1;
    }

    public void handleCollisions() {
        blocks.clear();
        final BoundingBox boundingBox = new BoundingBox(data.getPlayer())
                .expandSpecific(0, 0, 0.55, 0.6, 0, 0);

        this.boundingBox = boundingBox;

        final double minX = boundingBox.getMinX();
        final double minY = boundingBox.getMinY();
        final double minZ = boundingBox.getMinZ();
        final double maxX = boundingBox.getMaxX();
        final double maxY = boundingBox.getMaxY();
        final double maxZ = boundingBox.getMaxZ();

        for (double x = minX; x <= maxX; x += (maxX - minX)) {
            for (double y = minY; y <= maxY + 0.01; y += (maxY - minY) / 5) { //Expand max by 0.01 to compensate shortly for precision issues due to FP.
                for (double z = minZ; z <= maxZ; z += (maxZ - minZ)) {
                    final Location location = new Location(data.getPlayer().getWorld(), x, y, z);
                    final Block block = this.getBlock(location);
                    blocks.add(block);
                }
            }
        }

        handleClimbableCollision();
        handleVehicle();

        inLiquid = blocks.stream().anyMatch(Block::isLiquid);
        inWeb = blocks.stream().anyMatch(block -> block.getType() == Material.WEB);
        inAir = blocks.stream().allMatch(block -> block.getType() == Material.AIR);
        onIce = blocks.stream().anyMatch(block -> block.getType().toString().contains("ICE"));
        onSolidGround = blocks.stream().anyMatch(block -> block.getType().isSolid());
        nearSlab = blocks.stream().anyMatch(block -> block.getType().getData() == Step.class);
        nearStairs = blocks.stream().anyMatch(block -> block.getType().getData() == Stairs.class);
        nearTrapdoor = this.isCollidingAtLocation(1.801, material -> material == Material.TRAP_DOOR, CollisionType.ANY);
        blockNearHead = blocks.stream().filter(block -> block.getLocation().getY() - data.getPositionProcessor().getY() > 1.5)
                .anyMatch(block -> block.getType() != Material.AIR) || nearTrapdoor;
        onSlime = blocks.stream().anyMatch(block -> block.getType().toString().equalsIgnoreCase("SLIME_BLOCK"));
        nearPiston = blocks.stream().anyMatch(block -> block.getType().toString().contains("PISTON"));
        handleTicks();
    }

    public void handleClimbableCollision() {
        final Location location = data.getPlayer().getLocation();
        final int var1 = NumberConversions.floor(location.getX());
        final int var2 = NumberConversions.floor(location.getY());
        final int var3 = NumberConversions.floor(location.getZ());
        final Block var4 = this.getBlock(new Location(location.getWorld(), var1, var2, var3));
        this.onClimbable = var4.getType() == Material.LADDER || var4.getType() == Material.VINE;
    }


    public void handleVehicle() {
        try {
            nearVehicle = PlayerUtil.isNearVehicle(data.getPlayer());
        } catch (NoSuchElementException e) {
            // Kezeljük a kivételt, és állítsuk be a nearVehicle értékét false-ra
            nearVehicle = false;
        }
    }

    public void handleServerPosition(final WrappedPacketOutPosition wrapper) {
        final Vector teleportVector = new Vector(
                wrapper.getPosition().getX(),
                wrapper.getPosition().getY(),
                wrapper.getPosition().getZ()
        );

        teleports.add(teleportVector);
    }

    public boolean isColliding(CollisionType collisionType, Material blockType) {
        if (collisionType == CollisionType.ALL) {
            return blocks.stream().allMatch(block -> block.getType() == blockType);
        }
        return blocks.stream().anyMatch(block -> block.getType() == blockType);
    }

    public boolean isCollidingAtLocation(double drop, Predicate<Material> predicate, CollisionType collisionType) {
        final ArrayList<Material> materials = new ArrayList<>();

        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z+= 0.3) {
                final Material material = getBlock(data.getPlayer().getLocation().clone().add(x, drop, z)).getType();
                if (material != null) {
                    materials.add(material);
                }
            }
        }

        return collisionType == CollisionType.ALL ? materials.stream().allMatch(predicate) : materials.stream().allMatch(predicate);
    }

    //Taken from Fiona. If you have anything better, please let me know, thanks.
    public Block getBlock(final Location location) {
        if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return location.getBlock();
        } else {
            FutureTask<Block> futureTask = new FutureTask<>(() -> {
                location.getWorld().loadChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
                return location.getBlock();
            });
            Bukkit.getScheduler().runTask(QuantumTimeAC.INSTANCE.getPlugin(), futureTask);
            try {
                return futureTask.get();
            } catch (final Exception exception) {
                exception.printStackTrace();
            }
            return null;
        }
    }

    public  Block getBlockat(final World world, final int x, final int y, final int z) {
        if (world.isChunkLoaded(x >> 4, z >> 4)) {
            return world.getBlockAt(x, y, z);
        } else {
            FutureTask<Block> futureTask = new FutureTask<>(() -> {
                world.loadChunk(x >> 4, z >> 4);
                return world.getBlockAt(x, y, z);
            });
            Bukkit.getScheduler().runTask(QuantumTimeAC.INSTANCE.getPlugin(), futureTask);
            try {
                return futureTask.get();
            } catch (final Exception exception) {
                exception.printStackTrace();
            }
            return null;
        }
    }

    public int getSinceSpeedTicks() {
        return this.sinceSpeedTicks;
    }

    public void setSinceSpeedTicks(final int sinceSpeedTicks) {
        this.sinceSpeedTicks = sinceSpeedTicks;
    }

    public void handleOutgoingPacket(final Packet packet) {
        long currentTimeMillis = System.currentTimeMillis();
        if (packet.getPacketId() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            WrappedPacketOutNamedEntitySpawn wrapper = new WrappedPacketOutNamedEntitySpawn(packet.getRawPacket());

            Deque<LocationVector> deque = recentPlayerMoves.computeIfAbsent(wrapper.getEntityId(), id -> new ConcurrentLinkedDeque<>());
            deque.add(new LocationVector(wrapper.getPosition()));
            JavaV.trim(deque, 80);
        } else if (packet.getPacketId() == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrappedPacketOutEntityTeleport wrapper = new WrappedPacketOutEntityTeleport(packet.getRawPacket());
            Deque<LocationVector> deque = recentPlayerMoves.get(wrapper.getEntityId());
            if (deque != null) {
                deque.add(new LocationVector(wrapper.getPosition()));
                JavaV.trim(deque, 80);
            }
        } else if (packet.getPacketId() == PacketType.Play.Server.ENTITY) {
            final WrappedPacketOutEntity wrapper = new WrappedPacketOutEntity(packet.getRawPacket());

            Deque<LocationVector> deque = recentPlayerMoves.get(wrapper.getEntityId());
            if (deque != null && !deque.isEmpty()) {
                LocationVector move = move(deque.peekLast(), new LocationVector(wrapper.getDeltaX(), wrapper.getDeltaY(), wrapper.getDeltaZ()));
                move.setTimestamp(currentTimeMillis);
                deque.add(move);
                JavaV.trim(deque, 80);
            }
        }
    }


    public LocationVector move(final LocationVector customLocation, final LocationVector origin) {
        return customLocation.add(new LocationVector(origin.getX() / this.divider, origin.getY() / this.divider, origin.getZ() / this.divider));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <K, V> Map<K, V> createCache(Long l, Long l2) {
        CacheBuilder cacheBuilder = CacheBuilder.newBuilder();
        if (l != null) {
            cacheBuilder.expireAfterAccess(l.longValue(), TimeUnit.MILLISECONDS);
        }
        if (l2 != null) {
            cacheBuilder.expireAfterWrite(l2.longValue(), TimeUnit.MILLISECONDS);
        }
        return cacheBuilder.build().asMap();
    }

    private boolean isValidGroundLocation(Location location) {
        return location.getBlock().getRelative(BlockFace.DOWN).getType().isSolid() &&
               location.getBlock().getType().isTransparent() &&
               location.getBlock().getRelative(BlockFace.UP).getType().isTransparent();
    }

    public enum CollisionType {
        ANY, ALL
    }

}