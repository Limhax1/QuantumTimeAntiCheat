package com.gladurbad.medusa.data.processor;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.JavaV;
import com.gladurbad.medusa.util.type.LocationVector;
import com.google.common.cache.CacheBuilder;

import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityteleport.WrappedPacketOutEntityTeleport;
import io.github.retrooper.packetevents.packetwrappers.play.out.namedentityspawn.WrappedPacketOutNamedEntitySpawn;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import lombok.Getter;

@Getter
public class GlobalMovementProcessor {
    private Map<Integer, Deque<LocationVector>> recentPlayerMoves = createCache(TimeUnit.HOURS.toMillis(1L), null);   
    private final double divider = ServerVersion.getVersion().isNewerThan(ServerVersion.v_1_8_3) ? 4096.0 : 32.0;
    
    public void handleOutgoingPacket(final Packet packet, final PlayerData data) {
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
}
