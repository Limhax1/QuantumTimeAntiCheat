package com.gladurbad.medusa.check.impl.player.protocol;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Protocol (N)", description = "Checks for Disablers", complextype = "Disabler")
public class ProtocolN extends Check {
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private double timer = 0.0;

    public ProtocolN(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if(packet.isPosition() || packet.isPosLook()) {
            debug(timer);
            if(isExempt(ExemptType.TELEPORT)) {
                timer++;
            } else if(!isExempt(ExemptType.TELEPORT)) {
                timer = 0;
            }

            if(timer > 80) {
                fail("Tried to exploit teleport exemption");
                if(setback.getBoolean()) {
                    setback();
                }
            }
        }
    }
}
