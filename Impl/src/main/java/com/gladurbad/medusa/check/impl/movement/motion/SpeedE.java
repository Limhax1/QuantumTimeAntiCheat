package com.gladurbad.medusa.check.impl.movement.motion;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Speed (E)", description = "Checks for invalid Motion")
public class SpeedE extends Check {
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");
    public SpeedE(PlayerData data) {
        super(data);
    }

    double BUFFER = 0;
    double MAX_BUFFER = 3;

    @Override
    public void handle(Packet packet) {
        if(packet.isPosLook() || packet.isPosition()) {
            double expectedYMotion = getExpectedYMotion(data.getPlayer());
            boolean onGround = data.getPositionProcessor().isOnGround();
            boolean lastOnGround = data.getPositionProcessor().isLastOnGround();
            double deltaY = data.getPositionProcessor().getDeltaY();
            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.VELOCITY, ExemptType.SLIME, ExemptType.FLYING, ExemptType.UNDER_BLOCK, ExemptType.LIQUID, ExemptType.PISTON);

            double diff = data.getPositionProcessor().getLastY() - data.getPositionProcessor().getY();

            if (data.getPositionProcessor().getY() > 600 && !exempt && !onGround) {
                BUFFER++;
            }

            debug("Exp " + getExpectedYMotion(data.getPlayer()) + " " + data.getPositionProcessor().getY());

            if(BUFFER >= MAX_BUFFER) {
                if(setback.getBoolean()) {
                    setback();
                }
                fail("Exp " + getExpectedYMotion(data.getPlayer()) + " DY " + deltaY + " diff " + diff);
            }

        }
    }

    private double getExpectedYMotion(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.JUMP)) {
                int jumpAmplifier = effect.getAmplifier() + 1;
                if (jumpAmplifier == 1) return 0.5199999883770943;
                if (jumpAmplifier == 2) return 0.6199999898672104;
            }
        }
        return 0.41999998688697815;
    }

    private boolean isNearStairOrSlab(Player player) {
        int radius = 1;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = player.getLocation().add(x, y, z).getBlock();
                    Material type = block.getType();
                    if (type.name().contains("STAIRS") || type.name().contains("SLAB") || type.name().contains("STEP")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
