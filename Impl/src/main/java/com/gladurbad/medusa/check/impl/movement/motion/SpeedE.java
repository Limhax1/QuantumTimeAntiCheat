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
        if (packet.isPosLook() || packet.isPosition()) {

            //soonTM

        }
    }
}
