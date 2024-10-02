package com.gladurbad.medusa.data;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.processor.*;
import com.gladurbad.medusa.util.type.EvictingList;
import com.gladurbad.medusa.util.type.Pair;
import lombok.Getter;
import lombok.Setter;
import com.gladurbad.medusa.exempt.ExemptProcessor;
import com.gladurbad.medusa.manager.CheckManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public final class PlayerData {

    private final Player player;
    private String clientBrand;
    private int totalViolations, combatViolations, movementViolations, playerViolations;
    private final long joinTime = System.currentTimeMillis();
    private final List<Check> checks = CheckManager.loadChecks(this);
    private final EvictingList<Pair<Location, Integer>> targetLocations = new EvictingList<>(40);

    private final ExemptProcessor exemptProcessor = new ExemptProcessor(this);
    private final CombatProcessor combatProcessor = new CombatProcessor(this);
    private final ActionProcessor actionProcessor = new ActionProcessor(this);
    private final ClickProcessor clickProcessor = new ClickProcessor(this);
    private final PositionProcessor positionProcessor = new PositionProcessor(this);
    private final RotationProcessor rotationProcessor = new RotationProcessor(this);
    private final VelocityProcessor velocityProcessor = new VelocityProcessor(this);
    private final TransactionProcessor transactionProcessor = new TransactionProcessor(this);

    private int hurtTime = 0;
    private long lastHurtTime = 0;

    public void updateHurtTime() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHurtTime > 50) { // 50 ms = 1 tick
            if (hurtTime > 0) {
                hurtTime--;
            }
            lastHurtTime = currentTime;
        }
    }

    public void setHurtTime(int hurtTime) {
        this.hurtTime = Math.min(hurtTime, 10); // Maximum 10
        this.lastHurtTime = System.currentTimeMillis();
    }

    public int getHurtTime() {
        return hurtTime;
    }

    public PlayerData(final Player player) {
        this.player = player;
    }

    public Check getCheckByName(String checkName) {
        Optional<Check> check = checks.stream().filter(t -> t.getCheckInfo().name().equals(checkName)).findFirst();
        return check.isPresent() ? check.get() : null;
    }

    public void incrementHurtTime() {
        this.hurtTime = Math.min(this.hurtTime + 1, 10); // Maximum 10
        this.lastHurtTime = System.currentTimeMillis();
    }
}
