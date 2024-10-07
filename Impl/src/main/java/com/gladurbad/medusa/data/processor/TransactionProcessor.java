package com.gladurbad.medusa.data.processor;

import com.gladurbad.medusa.QuantumTimeAC;
import com.gladurbad.medusa.check.impl.player.protocol.ProtocolZ;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.keepalive.WrappedPacketOutKeepAlive;
import io.github.retrooper.packetevents.packetwrappers.play.in.keepalive.WrappedPacketInKeepAlive;
import io.github.retrooper.packetevents.utils.server.ServerVersion;

import org.bukkit.Bukkit;

import java.util.Map;
import java.util.HashMap;

public class TransactionProcessor {
    private final PlayerData data;

    private long keepAliveId = 0;
    private Long lastKeepAliveReceive = -1L;
    private Long lastKeepAliveSend = -1L;
    private final Map<Long, Long> keepAliveMap = new HashMap<>();
    private final ProtocolZ timeoutCheck;
    private final boolean isTransactionEnabled;

    public TransactionProcessor(PlayerData data) {
        this.data = data;
        this.timeoutCheck = (ProtocolZ) data.getCheckByName("Protocol (Z)");
        this.isTransactionEnabled = PacketEvents.get().getServerUtils().getVersion().isOlderThanOrEquals(ServerVersion.v_1_16_4);
    }

    public void handleIncoming(Packet packet) {
        if (!isTransactionEnabled) return;
        if (packet.getPacketId() != PacketType.Play.Client.KEEP_ALIVE) return;
        WrappedPacketInKeepAlive wrapper = new WrappedPacketInKeepAlive(packet.getRawPacket());
        long id = wrapper.getId();
        Long n = keepAliveMap.remove(id);
        if (n == null) return;
        timeoutCheck.debug("received " + id);
        if (id == keepAliveId) {
            lastKeepAliveReceive = System.currentTimeMillis();
        } else {
            QuantumTimeAC.INSTANCE.getPacketExecutor().execute(() -> timeoutCheck.fail("KeepAlive id mismatch " + id + " != " + keepAliveId));
        }
        keepAliveId++;
    }

    public void handleOutgoing(Packet packet) {
    }

    public void handleTransaction() {
        if (!isTransactionEnabled) return;
        Long n = System.currentTimeMillis();
        long diff = n - lastKeepAliveReceive;
        if (n - lastKeepAliveSend < 500 && diff > 30000 && lastKeepAliveReceive != -1 && lastKeepAliveSend != -1) {
            QuantumTimeAC.INSTANCE.getPacketExecutor().execute(() -> timeoutCheck.fail("Timed out " + diff + "ms"));
        }
        sendTransaction(keepAliveId);
        timeoutCheck.debug("sent: " + keepAliveId);
        lastKeepAliveSend = n;
        keepAliveMap.put(
                keepAliveId, lastKeepAliveSend);
        if (keepAliveId == Long.MAX_VALUE) {
            keepAliveId = 0;
        }
    }

    public void sendTransaction(long secret) {
        WrappedPacketOutKeepAlive wrapper = new WrappedPacketOutKeepAlive(secret);
        PacketEvents.get().getPlayerUtils().sendPacket(data.getPlayer(), wrapper);
    }
}