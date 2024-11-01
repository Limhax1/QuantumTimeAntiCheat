package com.gladurbad.medusa.check.impl.player.Scaffold;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Scaffold (B)", description = "Checks for Right Click Autoclickers.", complextype = "FastPlace")
public class ScaffoldB extends Check {
    public ScaffoldB(PlayerData data) {
        super(data);
    }

    private int ticks, cps;
    private final int[] cpsHistory = new int[5];
    private int historyIndex = 0;

    @Override
    public void handle(Packet packet) {
        if(packet.isFlying()) {
            if (++ticks >= 20) {
                cpsHistory[historyIndex] = cps;
                historyIndex = (historyIndex + 1) % 5;

                debug("RCps " + cps);

                if (areAllElementsEqual(cpsHistory) && cps > 7) {
                    fail("Constant RCps " + cps + " for 3 seconds");
                }

                ticks = cps = 0;
            }
        } else if (packet.isBlockPlace()) {
            ++cps;
        }
    }

    private boolean areAllElementsEqual(int[] array) {
        if (array.length == 0) return true;
        int first = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] != first) return false;
        }
        return true;
    }
}
