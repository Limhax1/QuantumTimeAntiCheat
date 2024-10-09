package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;

@CheckInfo(name = "Fly (D)", description = "Checks for Invalid Y motions", experimental = true, complextype = "InvalidY")
public class FlyD extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    public FlyD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if(packet.isPosition() || packet.isPosLook()) {

            boolean Exempt = isExempt(ExemptType.TELEPORT, ExemptType.JOINED, ExemptType.ELYTRA, ExemptType.BUBBLE_COLUMN);
            boolean notsurvival = !data.getPlayer().getGameMode().equals(GameMode.SURVIVAL);

            if(data.getPositionProcessor().getDeltaY() > 2.89 && !Exempt && !notsurvival) {
                if(setback.getBoolean()) {
                    setback();
                }
                fail("Attempted Vclip " + data.getPositionProcessor().getDeltaY());
            }
        }
    }
}
