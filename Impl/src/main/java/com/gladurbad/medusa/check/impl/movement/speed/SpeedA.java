package com.gladurbad.medusa.check.impl.movement.speed;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

@CheckInfo(name = "Speed (A)", description = "Checks for Speed.", complextype = "Speed")
public final class SpeedA extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    public SpeedA(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if(packet.isPosition() || packet.isPosLook()) {
            final double speed = data.getPositionProcessor().getDeltaXZ();
            boolean exempt2 = data.getPlayer().getItemInHand().containsEnchantment(Enchantment.ARROW_KNOCKBACK);
            if(isExempt(ExemptType.TELEPORT)) {
                debug(isExempt(ExemptType.TELEPORT));
            }
            if(speed > 0.65 && !data.getPlayer().hasPotionEffect(PotionEffectType.SPEED) && !isExempt(ExemptType.FLYING, ExemptType.VELOCITYEXC_FALL, ExemptType.TELEPORT, ExemptType.SLIME, ExemptType.PISTON, ExemptType.UNDER_BLOCK, ExemptType.ICE, ExemptType.JOINED, ExemptType.NEAR_VEHICLE, ExemptType.ELYTRA)) {
                buffer++;
                if(setback.getBoolean()) {
                    setback();
                }

                if(buffer >= max_buffer.getDouble()) {
                    fail("Going too Quick " + speed);
                }
            } else {
                buffer = buffer - buffer_decay.getDouble();
            }

            if(speed > 1.2 && !exempt2 && !isExempt(ExemptType.TELEPORT ,ExemptType.FLYING, ExemptType.SLIME, ExemptType.PISTON, ExemptType.ICE, ExemptType.JOINED, ExemptType.NEAR_VEHICLE, ExemptType.ELYTRA)) {
                buffer++;
                if(setback.getBoolean()) {
                    setback();
                }
                if(buffer >= max_buffer.getDouble()) {
                    fail("Going too Quick " + speed);
                }
            }
        }
    }
}