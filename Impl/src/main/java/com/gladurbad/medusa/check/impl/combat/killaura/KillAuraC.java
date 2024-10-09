package com.gladurbad.medusa.check.impl.combat.killaura;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

 
@CheckInfo(name = "KillAura (C)", description = "Checks for multi-aura.", complextype = "Multi")
public final class KillAuraC extends Check {

    public KillAuraC(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isUseEntity()) {
            final int targets = data.getCombatProcessor().getCurrentTargets();

            debug("tg="+ targets);
            if (targets > 1) fail("tg=" + targets);
        }
    }
}
