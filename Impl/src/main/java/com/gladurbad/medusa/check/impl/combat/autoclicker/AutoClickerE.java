package com.gladurbad.medusa.check.impl.combat.autoclicker;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.MathUtil;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckInfo(name = "AutoClicker (E)", description = "Detects consistent CPS and low deviation patterns.")
public final class AutoClickerE extends Check {

    private final Deque<Double> cpsValues = new ArrayDeque<>();
    private final Deque<Double> deviations = new ArrayDeque<>();
    private int ticks;
    private long lastClickTime;
    private static final int SAMPLE_SIZE = 20    ;
    private static final double CPS_RANGE_THRESHOLD = 0.5;
    private static final double LOW_DEVIATION_THRESHOLD = 5.0;
    private static final double BUFFER_LIMIT = 10.0;

    public AutoClickerE(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isArmAnimation() && !isExempt(ExemptType.AUTO_CLICKER)) {
            long currentTime = System.currentTimeMillis();
            if (lastClickTime != 0) {
                double cps = 1000.0 / (currentTime - lastClickTime);
                cpsValues.add(cps);
                
                if (cpsValues.size() == SAMPLE_SIZE) {
                    analyzeClicks();
                    cpsValues.removeFirst();
                }
            }
            lastClickTime = currentTime;
            ticks = 0;
        } else if (packet.isFlying()) {
            ++ticks;
        }
    }

    private void analyzeClicks() {
        double avgCps = MathUtil.getAverage(cpsValues);
        double minCps = cpsValues.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxCps = cpsValues.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double cpsRange = maxCps - minCps;
        
        double deviation = MathUtil.getStandardDeviation(cpsValues);
        
        deviations.add(deviation);
        if (deviations.size() > SAMPLE_SIZE) {
            deviations.removeFirst();
        }

        int lowDeviationCount = (int) deviations.stream().filter(d -> d < LOW_DEVIATION_THRESHOLD).count();

        boolean consistentCps = cpsRange < CPS_RANGE_THRESHOLD;
        boolean frequentLowDeviation = lowDeviationCount > SAMPLE_SIZE / 2;

        debug("AvgCPS: " + String.format("%.2f", avgCps) + 
              ", CPSRange: " + String.format("%.2f", cpsRange) + 
              ", Deviation: " + String.format("%.2f", deviation) + 
              ", LowDeviationCount: " + lowDeviationCount);

        if (consistentCps && frequentLowDeviation) {
            if ((buffer += 1) > BUFFER_LIMIT) {
                fail("Consistent CPS and low deviation detected. " +
                     "AvgCPS: " + String.format("%.2f", avgCps) + 
                     ", CPSRange: " + String.format("%.2f", cpsRange) + 
                     ", LowDeviationCount: " + lowDeviationCount);
                buffer = BUFFER_LIMIT;
            }
        } else {
            buffer = Math.max(buffer - 0.5, 0);
        }
    }
}
