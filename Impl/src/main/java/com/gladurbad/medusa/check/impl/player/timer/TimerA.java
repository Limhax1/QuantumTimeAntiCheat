package com.gladurbad.medusa.check.impl.player.timer;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.MathUtil;
import com.gladurbad.medusa.util.type.EvictingList;

@CheckInfo(name = "Timer (A)", description = "Checks for irregular game speed.", complextype = "Timer")
public class TimerA extends Check {

    private final EvictingList<Long> samples;
    private long lastFlying;
    private double buffer = 0d;

    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");
    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    
    public TimerA(final PlayerData data) {
        super(data);
        this.samples = new EvictingList<Long>(50);
    }
    
    @Override
    public void handle(final Packet packet) {
        if (packet.isFlying()) {
            debug("b=" + buffer);
            final long delay = System.currentTimeMillis() - this.lastFlying;
            final boolean exempt = this.isExempt(ExemptType.JOINED, ExemptType.TPS, ExemptType.INSIDE_VEHICLE, ExemptType.PISTON, ExemptType.TELEPORT);
            if (delay > 4L && !exempt) {
                this.samples.add(delay);
            }
            if (this.samples.isFull()) {
                final double average = MathUtil.getAverage(this.samples);
                final double speed = 50.0 / average;
                final double scaled = speed * 100.0;
                if (speed >= 1.05) {
                    if (buffer++ > max_buffer.getDouble()) {
                        this.fail("speed=" + scaled + "% delay=" + delay);
                        setback();
                        buffer = 0;
                    }
                }
                else {
                    if (buffer > 0) buffer -= 1.85; else buffer = 0; // math.max exists burr
                }
            }
            this.lastFlying = System.currentTimeMillis();
        }
        else if (packet.isTeleport()) {
            this.samples.add(150L);
        }
    }
}
