package com.gladurbad.medusa.check.impl.combat.autoclicker;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.MathUtil;
import com.google.common.collect.Lists;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "AutoClicker (E)", description = "Checks for too low variance.", complextype = "Variance")
public final class AutoClickerE extends Check {
    private final Deque<Long> samples;

    double BUFFER;
    private static final double MAX_BUFFER = 4;

    public AutoClickerE(final PlayerData data) {
        super(data);
        samples = Lists.newLinkedList();

    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isArmAnimation() && !this.isExempt(ExemptType.AUTO_CLICKER)) {
            final long delay = this.data.getClickProcessor().getDelay();
            if (delay > 5000L) {
                this.samples.clear();
                return;
            }
            this.samples.add(delay);
            if (this.samples.size() == 50) {
                final double variance = MathUtil.getVariance(this.samples);
                final double scaled = variance / 1000.0;
                debug(BUFFER);
                if (scaled < 28.2) {
                    if (BUFFER++ > MAX_BUFFER) {
                        fail("variance=" + scaled);
                    }
                }
                else {
                    BUFFER = Math.max(0, BUFFER - 0.25);
                }
                this.samples.clear();
            }
        }
    }

}
