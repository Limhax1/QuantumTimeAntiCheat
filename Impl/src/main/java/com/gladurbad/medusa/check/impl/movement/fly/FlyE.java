package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import static java.awt.SystemColor.info;

@CheckInfo(name = "Fly (E)", description = "Checks for Continuous ascension.", experimental = true, complextype = "Continuous ascension")
public class FlyE extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private static final double DELTA_Y_THRESHOLD = 0.0;
    private static final int MAX_CONSECUTIVE_SAME_DELTA_Y = 3;

    private double lastDeltaY = 0.0;
    private int consecutiveSameDeltaYCount = 0;

    public FlyE(final PlayerData data) {
        super(data);
    }


    @Override
    public void handle(Packet packet) {
        if (packet.isFlying()) {
            final double deltaY = data.getPositionProcessor().getDeltaY();
            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.FLYING, ExemptType.STEPPED, ExemptType.STAIRS, ExemptType.PISTON, ExemptType.LIQUID, ExemptType.CLIMBABLE, ExemptType.WEB);

            if (deltaY > DELTA_Y_THRESHOLD && deltaY == lastDeltaY && !exempt) {
                consecutiveSameDeltaYCount++;
                if (consecutiveSameDeltaYCount > MAX_CONSECUTIVE_SAME_DELTA_Y) {
                    if(setback.getBoolean()) {
                        setback();
                    }
                    fail("DY: " + deltaY + "Consecutive deltaY: " + consecutiveSameDeltaYCount);
                }
            } else {
                consecutiveSameDeltaYCount = 0;
            }

            String info = "DY: " + deltaY + "Consecutive deltaY: " + consecutiveSameDeltaYCount;

            debug(info);
            lastDeltaY = deltaY;
        }
    }
}
