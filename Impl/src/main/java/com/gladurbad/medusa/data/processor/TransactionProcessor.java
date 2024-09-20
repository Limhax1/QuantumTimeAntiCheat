package com.gladurbad.medusa.data.processor;

import org.bukkit.Bukkit;

import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.transaction.WrappedPacketInTransaction;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;

import java.util.Map;
import java.util.HashMap;

public class TransactionProcessor {
    private final PlayerData data;

    private Short transactionId = Short.MIN_VALUE;
    private Long lastTransactionReceive = -1L;
    private Long lastTransactionSend = -1L;
    private boolean received = true;
    private final Map<Short, Long> transactionMap = new HashMap<>();

    public TransactionProcessor(PlayerData data) {
        this.data = data;
    }

    public void handleIncoming(Packet packet) {
        if (packet.getPacketId() != PacketType.Play.Client.TRANSACTION) return;
        WrappedPacketInTransaction wrapper = new WrappedPacketInTransaction(packet.getRawPacket());
        short id = wrapper.getActionNumber();

        if (id == transactionId) {
            received = true;
            lastTransactionReceive = System.currentTimeMillis();
        } else {
            Bukkit.broadcastMessage("Invalid transactionID " + id + " != " + transactionId + " " + data.getPlayer().getName());
        }
        transactionId++;
    }

    public void handleOutgoing(Packet packet) {

    }

    public void handleTransaction() {
        Long n = System.currentTimeMillis();
        if (n - lastTransactionSend < 500 && n - lastTransactionReceive > 30000) {
            Bukkit.broadcastMessage("Transaction TimeOut " + data.getPlayer().getName());
        } 
        if (!received) return;
        received = false;
        sendTransaction(transactionId);
        lastTransactionSend = n;
        transactionMap.put(
            transactionId, lastTransactionSend);
        if (transactionId == Short.MAX_VALUE) {
            transactionId = Short.MIN_VALUE;
        }
    }

    public void sendTransaction(short secret) {
        WrappedPacketOutTransaction wrapper = new WrappedPacketOutTransaction(0, secret, false);
        PacketEvents.get().getPlayerUtils().sendPacket(data.getPlayer(), wrapper);
    }
}
