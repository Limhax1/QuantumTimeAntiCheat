package com.gladurbad.medusa.check.impl.combat.velocity;

import java.util.ArrayDeque;
import java.util.Deque;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@CheckInfo(name = "Velocity (B)", description = "Detects suspicious vertical movement patterns indicative of self-damage attempts.")
public class VelocityB extends Check {

    private static final int PATTERN_SIZE = 2;
    private static final double EPSILON = 1E-8;
    private static final int BUFFER_LIMIT = 0;

    private final Deque<Double> recentDeltaY = new ArrayDeque<>();
    private int buffer = 0;

    public boolean isSelfDamageDetected = false;
    private Location lastSafeLocation;
    private long setbackTime = 0;
    private static final long SETBACK_DURATION = 500; // 0.5 másodperc setback

    public VelocityB(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isPosition()) {
            Player player = data.getPlayer();
            double deltaY = data.getPositionProcessor().getDeltaY();

            if (System.currentTimeMillis() - setbackTime < SETBACK_DURATION) {
                if (lastSafeLocation != null) {
                    player.teleport(lastSafeLocation);
                }
                return;
            }

            recentDeltaY.addLast(deltaY);
            if (recentDeltaY.size() > PATTERN_SIZE) {
                recentDeltaY.removeFirst();
            }

            if (recentDeltaY.size() == PATTERN_SIZE) {
                boolean patternDetected = checkPattern();
                
                if (patternDetected && !isExempt(ExemptType.TELEPORT, ExemptType.INSIDE_VEHICLE, ExemptType.SLIME, ExemptType.UNDER_BLOCK)) {
                    if (++buffer > BUFFER_LIMIT) {
                        isSelfDamageDetected = true;
                        fail(String.format("Attempted Self damage: %.4f %.4f", recentDeltaY.getFirst(), recentDeltaY.getLast()));
                        setbackPlayer(player);
                        buffer = 0;
                    }
                } else {
                    buffer = Math.max(0, buffer - 1);
                    isSelfDamageDetected = false;
                }

                debug(String.format("DeltaY: %.8f, Pattern: %b, Buffer: %d, SelfDamageDetected: %b", 
                                    deltaY, patternDetected, buffer, isSelfDamageDetected));
            }

            if (!isSelfDamageDetected) {
                lastSafeLocation = player.getLocation();
            }
        }
    }

    private boolean checkPattern() {
        Double[] pattern = recentDeltaY.toArray(new Double[0]);
        double firstValue = pattern[0];
        double secondValue = pattern[1];

        return Math.abs(firstValue + secondValue) < EPSILON && Math.abs(firstValue) > 0.1;
    }

    private void setbackPlayer(Player player) {
        if (lastSafeLocation != null) {
            player.teleport(lastSafeLocation);
            setbackTime = System.currentTimeMillis();
        }
    }

    public boolean isSelfDamageDetected() {
        return isSelfDamageDetected;
    }
}