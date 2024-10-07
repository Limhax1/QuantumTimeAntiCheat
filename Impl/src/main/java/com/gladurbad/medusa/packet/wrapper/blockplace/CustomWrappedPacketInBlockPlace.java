package com.gladurbad.medusa.packet.wrapper.blockplace;

import io.github.retrooper.packetevents.packetwrappers.NMSPacket;
import io.github.retrooper.packetevents.packetwrappers.play.in.blockplace.WrappedPacketInBlockPlace;
import io.github.retrooper.packetevents.utils.player.Direction;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3f;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class CustomWrappedPacketInBlockPlace {

    private static final ServerVersion SERVER_VERSION = ServerVersion.getVersion();
    private final WrappedPacketInBlockPlace wrappedPacket;
    private final Player player;
    private boolean isAirPlace;
    private boolean isItemUse;

    public CustomWrappedPacketInBlockPlace(NMSPacket packet, Player player) {
        this.wrappedPacket = new WrappedPacketInBlockPlace(packet);
        this.player = player;
        this.isAirPlace = false;
        this.isItemUse = false;
        checkPlaceType();
    }

    private void checkPlaceType() {
        try {
            if (SERVER_VERSION.isNewerThanOrEquals(ServerVersion.v_1_18)) {
                Object movingObjectPositionBlock = readObjectFromWrappedPacket(wrappedPacket, 0);
                if (movingObjectPositionBlock == null) {
                    isAirPlace = true;
                    isItemUse = true;
                } else {
                    Vector3i blockPos = getBlockPosition();
                    Block targetBlock = player.getWorld().getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                    isItemUse = targetBlock.getType() != Material.AIR;
                }
            } else {
                Direction direction = wrappedPacket.getDirection();
                isAirPlace = direction == Direction.OTHER;
                if (isAirPlace) {
                    isItemUse = true;
                } else {
                    Vector3i blockPos = getBlockPosition();
                    Block targetBlock = player.getWorld().getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                    isItemUse = targetBlock.getType() != Material.AIR;
                }
            }
        } catch (Exception e) {
            isAirPlace = true;
            isItemUse = true;
        }
    }

    public boolean isAirPlace() {
        return isAirPlace;
    }

    public boolean isItemUse() {
        return isItemUse;
    }

    public Direction getDirection() {
        return isAirPlace ? Direction.OTHER : wrappedPacket.getDirection();
    }

    public Vector3i getBlockPosition() {
        if (isAirPlace) {
            Location loc = player.getLocation();
            return new Vector3i(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        }
        return wrappedPacket.getBlockPosition();
    }

    public Optional<Vector3f> getCursorPosition() {
        if (isAirPlace) {
            Location loc = player.getLocation();
            return Optional.of(new Vector3f((float)loc.getX(), (float)loc.getY(), (float)loc.getZ()));
        }
        return wrappedPacket.getCursorPosition();
    }

    private Object readObjectFromWrappedPacket(WrappedPacketInBlockPlace wrappedPacket, int index) throws Exception {
        try {
            Method readObjectMethod = WrappedPacketInBlockPlace.class.getSuperclass().getDeclaredMethod("readObject", int.class, Class.class);
            readObjectMethod.setAccessible(true);
            return readObjectMethod.invoke(wrappedPacket, index, Class.forName("net.minecraft.world.phys.MovingObjectPositionBlock"));
        } catch (Exception e) {
            // Ha nem találjuk a MovingObjectPositionBlock-ot, akkor null-t adunk vissza
            return null;
        }
    }

    private Object readObject(Object instance, String methodName) throws Exception {
        Method method = instance.getClass().getMethod(methodName);
        return method.invoke(instance);
    }

    private int readInt(Object instance, String methodName) throws Exception {
        Method method = instance.getClass().getMethod(methodName);
        return (int) method.invoke(instance);
    }

    private float readFloat(Object instance, String fieldName) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getFloat(instance);
    }
}
