package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Fly (D)", description = "Checks for Invalid Y motions", experimental = true)
public class FlyD extends Check {

    public FlyD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if(packet.isPosition() || packet.isPosLook()) {

            boolean Exempt = isExempt(ExemptType.TELEPORT, ExemptType.JOINED);

            if(data.getPositionProcessor().getDeltaY() > 3) {
                setback();
                fail("Attempted Vclip " + data.getPositionProcessor().getDeltaY());
            }
        }
    }
}
