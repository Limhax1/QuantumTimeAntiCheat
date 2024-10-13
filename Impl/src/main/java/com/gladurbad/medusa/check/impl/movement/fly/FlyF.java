package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Fly (F)", description = "Checks for impossible Y motions", complextype = "", experimental = true)
public class FlyF extends Check {

    public FlyF(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if(packet.isPosition() ||packet.isPosLook()) {
            double deltaY = data.getPositionProcessor().getDeltaY();
            double lastdeltaY = data.getPositionProcessor().getLastDeltaY();
            boolean exempt = isExempt(ExemptType.ELYTRA, ExemptType.TELEPORT, ExemptType.FLYING, ExemptType.ANYVELOCITY, ExemptType.LEVITATION);

        }
    }
}
