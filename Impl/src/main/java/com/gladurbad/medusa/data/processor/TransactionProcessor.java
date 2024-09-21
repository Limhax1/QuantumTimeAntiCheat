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
    private final Map<Short, Long> transactionMap = new HashMap<>();

    public TransactionProcessor(PlayerData data) {
        this.data = data;
    }

    public void handleIncoming(Packet packet) {
        if (packet.getPacketId() != PacketType.Play.Client.TRANSACTION) return;
        WrappedPacketInTransaction wrapper = new WrappedPacketInTransaction(packet.getRawPacket());
        short id = wrapper.getActionNumber();
        Long n = transactionMap.remove(id);
        System.out.println("received " + id);
        if (n == null) return;
        if (id == transactionId) {
            lastTransactionReceive = System.currentTimeMillis();
        } else {
            Bukkit.broadcastMessage("Invalid transactionID " + id + " != " + transactionId + " " + data.getPlayer().getName());
            data.getPlayer().kickPlayer(id + " != " + transactionId);
        }
        transactionId++;
    }

    public void handleOutgoing(Packet packet) {

    }

    public void handleTransaction() {
        Long n = System.currentTimeMillis();
        long diff = n - lastTransactionReceive;
        if (n - lastTransactionSend < 500 && diff > 5000 && lastTransactionReceive != -1 && lastTransactionSend != -1) {
            data.getPlayer().kickPlayer("Timed out " + diff + "ms");
        } 
        sendTransaction(transactionId);
        System.out.println("sent: " + transactionId);
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
