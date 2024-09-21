package com.gladurbad.medusa.check.impl.combat.reach;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CheckInfo(name = "Reach (C)", experimental = true, description = "Checks for reach hacks on the first attack")
public final class ReachC extends Check {

    private static final double MAX_REACH = 3.05;
    private static final long ATTACK_RESET_TIME = 500; // 500ms

    private double buffer;
    private long lastAttackTime;

    public ReachC(final PlayerData data) {
        super(data);
        this.buffer = 0.0;
        this.lastAttackTime = 0L;
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isUseEntity()) {
            WrappedPacketInUseEntity wrapper = new WrappedPacketInUseEntity(packet.getRawPacket());
            if (wrapper.getAction() != WrappedPacketInUseEntity.EntityUseAction.ATTACK || !(wrapper.getEntity() instanceof Player)) {
                return;
            }

            Player player = data.getPlayer();
            Player target = (Player) wrapper.getEntity();

            long now = System.currentTimeMillis();
            long timeSinceLastAttack = now - lastAttackTime;

            if (timeSinceLastAttack > ATTACK_RESET_TIME) {
                Vector origin = player.getLocation().toVector();
                Vector targetPos = target.getLocation().toVector();

                double distance = origin.distance(targetPos) - 0.5; // Subtracting 0.3 to account for hitbox

                boolean knockback = player.getInventory().getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK) > 0;
                boolean exempt = isExempt(ExemptType.INSIDE_VEHICLE);
                boolean targetExempt = isExempt(ExemptType.INSIDE_VEHICLE);

                debug("dist=" + String.format("%.2f", distance) + 
                      " max=" + String.format("%.2f", MAX_REACH) + 
                      " buffer=" + String.format("%.2f", buffer));

                if (distance > MAX_REACH && distance < 6.0 && !knockback && !exempt && !targetExempt) {
                    buffer += 1.0;
                    if (buffer > 3) {
                        fail("distance=" + String.format("%.2f", distance));
                        buffer = 3;
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.5);
                }
            }

            lastAttackTime = now;
        }
    }
}