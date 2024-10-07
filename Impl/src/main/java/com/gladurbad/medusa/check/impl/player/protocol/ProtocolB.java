package com.gladurbad.medusa.check.impl.player.protocol;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import io.github.retrooper.packetevents.packetwrappers.play.in.abilities.WrappedPacketInAbilities;

@CheckInfo(name = "Protocol (B)", description = "Checks for spoofed abilities packets.", complextype = "Spoofed abilities")
public final class ProtocolB extends Check {

    public ProtocolB(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isAbilities()) {
            final WrappedPacketInAbilities wrapper = new WrappedPacketInAbilities(packet.getRawPacket());

            wrapper.isFlightAllowed().ifPresent(flightAllowed -> {
                final boolean invalid = flightAllowed && !data.getPlayer().getAllowFlight();

                if (invalid) {
                    fail();
                }
            });
        }
    }
}
