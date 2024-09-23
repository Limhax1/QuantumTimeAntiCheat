package com.gladurbad.medusa.check.impl.player.protocol;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;

/**
    * I came
 **/

@CheckInfo(name = "Protocol (L)", description = "Checks for players who took the L. (groundspoof xd)")
public final class ProtocolL extends Check {

    public ProtocolL(final PlayerData data) {
        super(data);
    }

    int buffer = 0;

    @Override
    public void handle(final Packet packet) {
        if (!packet.isPosition()) {
            return;
        }

        boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.NEAR_VEHICLE, ExemptType.JOINED, ExemptType.PISTON, ExemptType.SLIME);

        if (exempt) return;

        WrappedPacketInFlying wrapper = new WrappedPacketInFlying(packet.getRawPacket());
        boolean spoofedGround = wrapper.isOnGround();
        boolean realGround = data.getPositionProcessor().isMathematicallyOnGround();

        String info = "spoofedGround: " + spoofedGround + " realGround: " + realGround;

        debug(info);

        if (spoofedGround != realGround && buffer++ > 3) {
            buffer = 0;
            setbackLastGround();
            fail(info);
        }
    }
}
