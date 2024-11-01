package com.gladurbad.medusa.listener;

import com.gladurbad.medusa.QuantumTimeAC;
import com.gladurbad.medusa.data.PlayerData;

import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import com.gladurbad.medusa.packet.Packet;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;


public final class NetworkListener extends PacketListenerDynamic {

    @Override
    public void onPacketPlayReceive(final PacketPlayReceiveEvent event) {
        final PlayerData data = QuantumTimeAC.INSTANCE.getPlayerDataManager().getPlayerData(event.getPlayer());

        handle: {
            if (data == null) break handle;

            QuantumTimeAC.INSTANCE.getPacketExecutor().execute(() -> QuantumTimeAC.INSTANCE.getReceivingPacketProcessor()
                    .handle(data, new Packet(Packet.Direction.RECEIVE, event.getNMSPacket(), event.getPacketId())));
        }
    }

    @Override
    public void onPacketPlaySend(final PacketPlaySendEvent event) {
        final PlayerData data = QuantumTimeAC.INSTANCE.getPlayerDataManager().getPlayerData(event.getPlayer());

        handle: {
            if (data == null) break handle;

            QuantumTimeAC.INSTANCE.getPacketExecutor().execute(() -> QuantumTimeAC.INSTANCE.getSendingPacketProcessor()
                    .handle(data, new Packet(Packet.Direction.SEND, event.getNMSPacket(), event.getPacketId())));
        }
    }
}
