package com.gladurbad.medusa.check.impl.player.Scaffold;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

@CheckInfo(name = "Scaffold (A)", description = "Detects suspicious block placement patterns and timings.")
public class ScaffoldA extends Check {
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private final Deque<Long> placeTimes = new LinkedList<>();
    private final Deque<Double> yawChanges = new LinkedList<>();
    private final Deque<Double> pitchChanges = new LinkedList<>();

    private static final int SAMPLE_SIZE = 15;
    private static final double MAX_YAW_CHANGE = 18.0; // Csökkentve 20.0-ról
    private static final double MAX_PITCH_CHANGE = 18.0; // Csökkentve 20.0-ról
    private static final long MIN_PLACE_DELAY = 35; // Csökkentve 40-ről
    private static final double MAX_REACH = 4.5;

    private long lastPlaceTime = 0;
    private float lastYaw = 0;
    private float lastPitch = 0;
    private double buffer = 0;
    private int consistentPlacements = 0;

    public boolean placedBlock;

    public ScaffoldA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isFlying()) {
            if (placedBlock && isBridging()) {
                long now = System.currentTimeMillis();
                long delay = now - lastPlaceTime;

                float yaw = data.getPlayer().getLocation().getYaw();
                float pitch = data.getPlayer().getLocation().getPitch();

                double yawChange = Math.abs(yaw - lastYaw);
                double pitchChange = Math.abs(pitch - lastPitch);

                placeTimes.addLast(delay);
                yawChanges.addLast(yawChange);
                pitchChanges.addLast(pitchChange);

                if (placeTimes.size() > SAMPLE_SIZE) {
                    placeTimes.removeFirst();
                    yawChanges.removeFirst();
                    pitchChanges.removeFirst();
                }

                if (delay < MIN_PLACE_DELAY && yawChange < MAX_YAW_CHANGE && pitchChange < MAX_PITCH_CHANGE) {
                    consistentPlacements++;
                } else {
                    consistentPlacements = 0;
                }

                if (consistentPlacements > SAMPLE_SIZE) {
                    fail("Scaffold detected: consistent block placements with minimal yaw/pitch changes.");
                    if (setback.getBoolean()) {
                        setback();
                    }
                    consistentPlacements = 0;
                }

                lastPlaceTime = now;
                lastYaw = yaw;
                lastPitch = pitch;
            }
            placedBlock = false;
        } else if (packet.isBlockPlace()) {
            if (data.getPlayer().getItemInHand().getType().isBlock()) {
                placedBlock = true;
            }
        }
    }
}
