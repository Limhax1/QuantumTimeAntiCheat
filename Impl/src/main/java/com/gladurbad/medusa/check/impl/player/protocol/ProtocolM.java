package com.gladurbad.medusa.check.impl.player.protocol;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.PlayerUtil;

@CheckInfo(name = "Protocol (M)", description = "Checks for blink.", experimental = true, complextype = "Blink")
public class ProtocolM extends Check {

    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    public ProtocolM(PlayerData data) {
        super(data);
    }

    private int blinkTicks;
    private long lastPositionPacket;

    @Override
    public void handle(Packet packet) {

        boolean exempt = isExempt(ExemptType.JOINED, ExemptType.TPS, ExemptType.TELEPORT);

        long now = System.currentTimeMillis();

        if (packet.isPosition() || packet.isPosLook() || packet.isFlying() || data.getPlayer().isDead()) {
            long timeDiff = now - lastPositionPacket;
            if (timeDiff > 1) {
                blinkTicks = 0;
            }
            lastPositionPacket = now;
        } else if(!exempt && !data.getPlayer().isDead() && data.getPlayer().getTicksLived() > 10) {
            blinkTicks++;
        }

        if (blinkTicks % 3 == 0) {
            debug( blinkTicks + PlayerUtil.getPing(data.getPlayer()) / 5);
        }

        if (blinkTicks > 175 + PlayerUtil.getPing(data.getPlayer()) / 5 && !exempt) {
            fail("Last position was: " + blinkTicks / 3 + " ticks ago");
            if (setback.getBoolean()) {
                setback();
            }
            blinkTicks = 0;
        }
    }
}
