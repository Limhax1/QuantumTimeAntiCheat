package com.gladurbad.medusa.data.processor;

import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.in.transaction.WrappedPacketInTransaction;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import lombok.Getter;
import com.gladurbad.medusa.QuantumTimeAC;
import com.gladurbad.medusa.data.PlayerData;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public final class VelocityProcessor {

    private final PlayerData data;
    private double velocityX, velocityY, velocityZ, lastVelocityX, lastVelocityY, lastVelocityZ;
    private int maxVelocityTicks, velocityTicks, ticksSinceVelocity;
    private short transactionID, velocityID;
    private long transactionPing, transactionReply;
    private boolean verifyingVelocity;
    private boolean useTransactions;

    public VelocityProcessor(final PlayerData data) {
        this.data = data;
        this.useTransactions = PacketEvents.get().getServerUtils().getVersion().isNewerThanOrEquals(ServerVersion.v_1_12);
    }

    public void handle(final double velocityX, final double velocityY, final double velocityZ) {
        this.ticksSinceVelocity = 0;

        lastVelocityX = this.velocityX;
        lastVelocityY = this.velocityY;
        lastVelocityZ = this.velocityZ;

        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;

        if (useTransactions) {
            this.velocityID = (short) ThreadLocalRandom.current().nextInt(32767);
            this.verifyingVelocity = true;
            sendTransaction(velocityID);
        } else {
            this.velocityTicks = QuantumTimeAC.INSTANCE.getTickManager().getTicks();
            this.maxVelocityTicks = (int) (((lastVelocityZ + lastVelocityX) / 2 + 2) * 15);
        }
    }

    public void handleTransaction(final WrappedPacketInTransaction wrapper) {
        if (!useTransactions) return;

        if (this.verifyingVelocity && wrapper.getActionNumber() == this.velocityID) {
            this.verifyingVelocity = false;
            this.velocityTicks = QuantumTimeAC.INSTANCE.getTickManager().getTicks();
            this.maxVelocityTicks = (int) (((lastVelocityZ + lastVelocityX) / 2 + 2) * 15);
        }

        if (wrapper.getActionNumber() == transactionID) {
            transactionPing = System.currentTimeMillis() - transactionReply;

            transactionID = (short) ThreadLocalRandom.current().nextInt(32767);
            sendTransaction(transactionID);
            transactionReply = System.currentTimeMillis();
        }
    }

    private void sendTransaction(short id) {
        if (useTransactions) {
            try {
                PacketEvents.get().getPlayerUtils().sendPacket(data.getPlayer(), 
                    new io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction(0, id, false));
            } catch (Exception e) {
                // Ha a WrappedPacketOutTransaction nem támogatott, kapcsoljuk ki a tranzakciókat
                useTransactions = false;
            }
        }
    }

    public void handleFlying() {
        ++ticksSinceVelocity;
    }

    public boolean isTakingVelocity() {
        return Math.abs(QuantumTimeAC.INSTANCE.getTickManager().getTicks() - this.velocityTicks) < this.maxVelocityTicks;
    }
}
