package com.gladurbad.medusa.check.impl.movement.antiweb;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Antiweb (A)", description = "Checks for going too quick in cobwebs", complextype = "Speed")
public class AntiwebA extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    double webtime = 0;

    public AntiwebA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if(packet.isPosLook() || packet.isPosition()) {
            double deltaXZ = data.getPositionProcessor().getDeltaXZ();
            double speed = getSpeedPotionCorrection(data.getPlayer());
            boolean inweb = data.getPlayer().getLocation().getBlock().getType().equals(Material.WEB);
            boolean invalid = deltaXZ > 0.04 * speed;
            boolean exempt = isExempt(
                    ExemptType.ELYTRA, ExemptType.TELEPORT,
                    ExemptType.FLYING, ExemptType.ANYVELOCITY
                    );

            debug(buffer + " " + invalid);

            if(inweb) {
                webtime++;
            } else {
                webtime = 0;
            }

            if(inweb && webtime > 10 && !exempt) {
                if(invalid) {
                    buffer++;

                    if(buffer > max_buffer.getDouble()) {
                        fail("dXZ: " + deltaXZ);
                        buffer = 0;

                        if(setback.getBoolean()) {
                            setback();
                        }
                    }
                } else {
                    buffer = Math.max(0, buffer-buffer_decay.getDouble());
                }
            } else {
                buffer = Math.max(0, buffer-buffer_decay.getDouble());
            }

        }
    }

    private double getSpeedPotionCorrection(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.SPEED)) {
                int speedAmplifier = effect.getAmplifier() + 1;
                return 1.0 + (0.2 * speedAmplifier);
            }
        }
        return 1.0;
    }
}
